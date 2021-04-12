package bm;

import am1.Metrics;
import av.HiveMQConfigurationService;
import av.Internals;
import bi.CachedMessages;
import bo.ClusterPublish;
import bo.SendStatus;
import bu.InternalPublish;
import cb1.AttributeKeys;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.SettableFuture;
import com.hivemq.spi.annotations.NotNull;
import com.hivemq.spi.message.MessageWithId;
import com.hivemq.spi.message.PubAck;
import com.hivemq.spi.message.PubComp;
import com.hivemq.spi.message.Publish;
import com.hivemq.spi.message.QoS;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;

public class MqttOrderedTopicHandler extends ChannelDuplexHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(MqttOrderedTopicHandler.class);
    private final Metrics metrics;
    private final CachedMessages cachedMessages = CachedMessages.INSTANCE;
    private final int inFlightParallelTopicCount;
    private final Map<Integer, Integer> inFlightMessageIdStore = new HashMap<>();
    private final Queue<InFlightMessage>[] inFlightParallelMessageQueues;
    private final boolean[] inFlightParallelUseStatus;
    @VisibleForTesting
    protected long maxQueuedMessages;

    @Inject
    public MqttOrderedTopicHandler(HiveMQConfigurationService hiveMQConfigurationService,
                                   Metrics metrics) {
        this.metrics = metrics;
        this.inFlightParallelTopicCount = hiveMQConfigurationService.internalConfiguration().getInt(Internals.MQTT_INFLIGHT_PARALLEL_TOPIC_COUNT);
        this.maxQueuedMessages = hiveMQConfigurationService.mqttConfiguration().maxQueuedMessages();
        this.inFlightParallelMessageQueues = new ArrayDeque[this.inFlightParallelTopicCount];
        this.inFlightParallelUseStatus = new boolean[this.inFlightParallelTopicCount];
    }

    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof PubAck || msg instanceof PubComp) {
            if (this.inFlightMessageIdStore.size() == this.maxQueuedMessages / 2L) {
                this.metrics.halfFullQueueCount().dec();
            }
            Integer messageId = this.inFlightMessageIdStore.remove(((MessageWithId) msg).getMessageId());
            if (messageId == null) {
                super.channelRead(ctx, msg);
                return;
            }
            if (this.inFlightParallelMessageQueues[messageId] == null ||
                    this.inFlightParallelMessageQueues[messageId].isEmpty()) {
                this.inFlightParallelUseStatus[messageId] = false;
                super.channelRead(ctx, msg);
                return;
            }
            Queue<InFlightMessage> queue = this.inFlightParallelMessageQueues[messageId];
            InFlightMessage inFlightMessage = queue.poll();
            ctx.writeAndFlush(inFlightMessage.getPublish(), inFlightMessage.getPromise());
        }
        super.channelRead(ctx, msg);
    }

    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        SettableFuture<SendStatus> settableFuture = null;
        if (msg instanceof ClusterPublish) {
            settableFuture = ((ClusterPublish) msg).getSettableFuture();
        }
        if (!(msg instanceof InternalPublish) ||
                ((InternalPublish) msg).getQoS().getQosNumber() <= 0) {
            super.write(ctx, msg, promise);
            return;
        }
        InternalPublish publish = (InternalPublish) msg;
        String topic = publish.getTopic();
        int parallelIndex = getParallelIndex(topic, this.inFlightParallelTopicCount);
        if (publish.isDuplicateDelivery() &&
                this.inFlightMessageIdStore.containsKey(publish.getMessageId())) {
            if (settableFuture != null) {
                settableFuture.set(SendStatus.DELIVERED);
            }
            super.write(ctx, msg, promise);
            return;
        }
        if (this.inFlightMessageIdStore.size() >= this.maxQueuedMessages &&
                this.maxQueuedMessages >= 0L) {
            if (LOGGER.isDebugEnabled()) {
                String clientId = ctx.channel().attr(AttributeKeys.MQTT_CLIENT_ID).get();
                LOGGER.debug("A message for client {} and topic {}, has been dropped. There are too many in-flight messages for that client: {}.",
                        clientId, publish.getTopic(), this.inFlightMessageIdStore.size());
            }
            this.metrics.droppedMessageCount().inc();
            this.metrics.droppedMessageRate().mark();
            if (publish.getQoS() == QoS.AT_LEAST_ONCE) {
                ctx.pipeline().fireChannelRead(this.cachedMessages.getPubAck(publish.getMessageId()));
            } else if (publish.getQoS() == QoS.EXACTLY_ONCE) {
                ctx.pipeline().fireChannelRead(this.cachedMessages.getPubComp(publish.getMessageId()));
            }
            if (settableFuture != null) {
                settableFuture.set(SendStatus.FAILED);
            }
            promise.setSuccess();
            return;
        }
        if (this.inFlightMessageIdStore.size() == this.maxQueuedMessages / 2L - 1L) {
            this.metrics.halfFullQueueCount().inc();
        }
        if (!this.inFlightParallelUseStatus[parallelIndex]) {
            this.inFlightParallelUseStatus[parallelIndex] = true;
            this.inFlightMessageIdStore.put(publish.getMessageId(), parallelIndex);
            if (settableFuture != null) {
                settableFuture.set(SendStatus.DELIVERED);
            }
            super.write(ctx, msg, promise);
            return;
        }
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Enqueued publish message with qos {} messageId {} and topic {} for client {}",
                    ((Publish) msg).getQoS().name(),
                    ((Publish) msg).getMessageId(),
                    ((Publish) msg).getTopic(),
                    ctx.channel().attr(AttributeKeys.MQTT_CLIENT_ID).get());
        }
        Queue<InFlightMessage> queue = getInFlightMessageQueue(parallelIndex);
        queue.add(new InFlightMessage(publish, promise));
        this.inFlightMessageIdStore.put(publish.getMessageId(), parallelIndex);
        if (settableFuture != null) {
            settableFuture.set(SendStatus.DELIVERED);
        }
    }

    private Queue<InFlightMessage> getInFlightMessageQueue(int parallelIndex) {
        Queue<InFlightMessage> queue = this.inFlightParallelMessageQueues[parallelIndex];
        if (queue == null) {
            this.inFlightParallelMessageQueues[parallelIndex] = new ArrayDeque();
            queue = this.inFlightParallelMessageQueues[parallelIndex];
        }
        return queue;
    }

    @VisibleForTesting
    protected Queue<InFlightMessage>[] getInFlightParallelMessageQueues() {
        return this.inFlightParallelMessageQueues;
    }

    public static int getParallelIndex(@NotNull String topic, int parallelCount) {
        return Math.abs(topic.hashCode() % parallelCount);
    }

    @VisibleForTesting
    static class InFlightMessage {
        @NotNull
        private final InternalPublish publish;
        @NotNull
        private final ChannelPromise promise;

        public InFlightMessage(@NotNull InternalPublish publish,
                               @NotNull ChannelPromise promise) {
            this.publish = publish;
            this.promise = promise;
        }

        @NotNull
        public InternalPublish getPublish() {
            return publish;
        }

        @NotNull
        public ChannelPromise getPromise() {
            return promise;
        }
    }
}
