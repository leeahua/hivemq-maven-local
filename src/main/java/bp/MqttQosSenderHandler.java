package bp;

import bi.CachedMessages;
import bn1.OutgoingMessageFlowSinglePersistence;
import bu.InternalPublish;
import cb1.AttributeKeys;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.hivemq.spi.message.Connect;
import com.hivemq.spi.message.MessageWithId;
import com.hivemq.spi.message.PubAck;
import com.hivemq.spi.message.PubComp;
import com.hivemq.spi.message.PubRec;
import com.hivemq.spi.message.PubRel;
import com.hivemq.spi.services.configuration.MqttConfigurationService;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.util.concurrent.ScheduledFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

// TODO:
public class MqttQosSenderHandler extends ChannelDuplexHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(MqttQosSenderHandler.class);
    private static final int b = 1048576;
    private final OutgoingMessageFlowSinglePersistence outgoingMessageFlowSinglePersistence;
    private final Integer retryInterval;
    private final CachedMessages cachedMessages = CachedMessages.INSTANCE;
    private final Map<Integer, ScheduledFuture> timeouts = new ConcurrentHashMap<>();

    @Inject
    MqttQosSenderHandler(OutgoingMessageFlowSinglePersistence outgoingMessageFlowSinglePersistence,
                         MqttConfigurationService mqttConfigurationService) {
        this.outgoingMessageFlowSinglePersistence = outgoingMessageFlowSinglePersistence;
        this.retryInterval = mqttConfigurationService.retryInterval();
    }

    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof Connect) {
            return;
        }
        String clientId = ctx.channel().attr(AttributeKeys.MQTT_CLIENT_ID).get();
        if (msg instanceof PubAck) {
            handleRead((PubAck) msg, clientId);
        } else if (msg instanceof PubRec) {
            handleRead(ctx, (PubRec) msg, clientId);
        } else if (msg instanceof PubComp) {
            handleRead((PubComp) msg, clientId);
        } else {
            super.channelRead(ctx, msg);
        }
    }

    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
        String clientId = ctx.channel().attr(AttributeKeys.MQTT_CLIENT_ID).get();
        ListenableFuture future;
        if (msg instanceof InternalPublish) {
            future = handleWrite(ctx, (InternalPublish) msg, promise, clientId);
        } else if (msg instanceof PubRel) {
            future = handleWrite(ctx, (PubRel) msg, promise, clientId);
        } else {
            future = Futures.immediateFuture(null);
        }
        Futures.addCallback(future, new FutureCallback<Void>() {
            @Override
            public void onSuccess(@Nullable Void result) {
                try {
                    ctx.writeAndFlush(msg, promise);
                } catch (Exception e) {
                    LOGGER.error("Exception in channel write.", e);
                }
            }

            @Override
            public void onFailure(Throwable t) {
                try {
                    ctx.writeAndFlush(msg, promise);
                } catch (Exception e) {
                    LOGGER.error("Exception in channel write.", e);
                }
            }
        }, ctx.channel().eventLoop());
    }

    private ListenableFuture<Void> handleWrite(ChannelHandlerContext ctx, PubRel pubRel, ChannelPromise promise, String clientId) {
        ListenableFuture<Void> future = this.outgoingMessageFlowSinglePersistence.addOrReplace(clientId, pubRel.getMessageId(), pubRel);
        SettableFuture<Void> settableFuture = SettableFuture.create();
        Futures.addCallback(future, new FutureCallback<Void>() {

            @Override
            public void onSuccess(@Nullable Void result) {
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("Client {}: Sending PubRel Message", clientId);
                }
                promise.addListener(b(ctx, clientId, pubRel));
                settableFuture.set(null);
            }

            @Override
            public void onFailure(Throwable t) {
                LOGGER.error("Exception while adding message to message flow.", t);
                settableFuture.set(null);
            }
        }, ctx.executor());
        return settableFuture;
    }

    private ListenableFuture<Void> handleWrite(ChannelHandlerContext ctx, InternalPublish publish, ChannelPromise promise, String clientId) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Client {}: Sending Publish QoS {} Message",
                    clientId, publish.getQoS().getQosNumber());
        }
        if (publish.getQoS().getQosNumber() > 0) {
            if (publish.getPayload().length <= 7340032) {
                SettableFuture settableFuture = SettableFuture.create();
                ListenableFuture future = this.outgoingMessageFlowSinglePersistence.addOrReplace(clientId, publish.getMessageId(), publish);
                Futures.addCallback(future, new FutureCallback<Void>() {

                    @Override
                    public void onSuccess(@Nullable Void result) {
                        promise.addListener(b(ctx, clientId, publish));
                        settableFuture.set(null);
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        LOGGER.error("Exception while adding message to message flow.", t);
                        settableFuture.set(null);
                    }
                }, ctx.executor());
                return settableFuture;
            }
            promise.addListener(b(ctx, clientId, publish));
        }
        return Futures.immediateFuture(null);
    }

    private void handleRead(PubComp pubComp, String clientId) {
        LOGGER.trace("Client {}: Received PubComp", clientId);
        cancelTimeout(pubComp);
        this.outgoingMessageFlowSinglePersistence.remove(clientId, pubComp.getMessageId());
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Client {}: Received PubComp remove message id:[{}] inflight size: {}",
                    clientId, pubComp.getMessageId(), this.outgoingMessageFlowSinglePersistence.size(clientId));
        }
    }


    private void handleRead(PubAck pubAck, String clientId) {
        LOGGER.trace("Client {}: Received PubAck", clientId);
        int messageId = pubAck.getMessageId();
        cancelTimeout(pubAck);
        this.outgoingMessageFlowSinglePersistence.remove(clientId, messageId);
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Client {}: Received PubAck remove message id:[{}] inflight size: {}",
                    clientId, messageId, this.outgoingMessageFlowSinglePersistence.size(clientId));
        }
    }

    private void handleRead(ChannelHandlerContext ctx, PubRec pubRec, String clientId) {
        LOGGER.trace("Client {}: Received PubRec", clientId);
        cancelTimeout(pubRec);
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Client {}: Received PubRec remove message id:[{}] inflight size: {}",
                    clientId, pubRec.getMessageId(), this.outgoingMessageFlowSinglePersistence.size(clientId));
        }
        ctx.channel().writeAndFlush(this.cachedMessages.getPubRel(pubRec.getMessageId()));
    }

    private void a(ChannelHandlerContext ctx, String clientId, InternalPublish publish) {
        ScheduledFuture<?> scheduledFuture = ctx.channel().eventLoop()
                .schedule(a(ctx.channel(), publish.getMessageId()), this.retryInterval, TimeUnit.SECONDS);
        this.timeouts.put(publish.getMessageId(), scheduledFuture);
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Client {}: Sent Publish QoS {} add message id [{}] inflight size: [{}]",
                    clientId, publish.getQoS().getQosNumber(), publish.getMessageId(), this.outgoingMessageFlowSinglePersistence.size(clientId));
        }
    }

    private void a(ChannelHandlerContext ctx, String clientId, PubRel pubRel) {
        ScheduledFuture timeout = ctx.channel().eventLoop()
                .schedule(a(ctx.channel(), pubRel.getMessageId()), this.retryInterval, TimeUnit.SECONDS);
        this.timeouts.put(pubRel.getMessageId(), timeout);
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Client {}: Sent PubRel add message id [{}] inflight size: [{}]",
                    clientId, pubRel.getMessageId(), this.outgoingMessageFlowSinglePersistence.size(clientId));
        }
    }

    private ChannelFutureListener b(ChannelHandlerContext ctx, String clientId, InternalPublish publish) {
        return future -> a(ctx, clientId, publish);
    }

    private ChannelFutureListener b(ChannelHandlerContext ctx, String clientId, PubRel pubRel) {
        return future -> a(ctx, clientId, pubRel);
    }

    private Runnable a(Channel channel, Integer messageId) {
        return () -> {
            if (!channel.isOpen()) {
                return;
            }
            String clientId = channel.attr(AttributeKeys.MQTT_CLIENT_ID).get();
            MessageWithId messageWithId = outgoingMessageFlowSinglePersistence.get(clientId, messageId);
            if (messageWithId == null) {
                return;
            }
            if (messageWithId instanceof InternalPublish) {
                InternalPublish publish = (InternalPublish) messageWithId;
                publish.setDuplicateDelivery(true);
                channel.writeAndFlush(publish);
            } else {
                channel.writeAndFlush(messageWithId);
            }
        };
    }

    private void cancelTimeout(MessageWithId messageWithId) {
        ScheduledFuture timeout = this.timeouts.remove(messageWithId.getMessageId());
        if (timeout != null) {
            timeout.cancel(false);
        }
    }
}
