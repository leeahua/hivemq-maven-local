package bj;

import bl1.ChannelPersistence;
import bm1.QueuedMessagesSinglePersistence;
import bn1.OutgoingMessageFlowSinglePersistence;
import bu.InternalPublish;
import bu.MessageIDPools;
import bw.ProduceMessageIdException;
import cb1.PluginExceptionUtils;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.hivemq.spi.annotations.NotNull;
import com.hivemq.spi.message.MessageWithId;
import com.hivemq.spi.message.PubRec;
import com.hivemq.spi.message.PubRel;
import com.hivemq.spi.message.Publish;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

// TODO:
public class SendOutMessagesFutureListener implements ChannelFutureListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(SendOutMessagesFutureListener.class);
    private final QueuedMessagesSinglePersistence queuedMessagesSinglePersistence;
    private final OutgoingMessageFlowSinglePersistence outgoingMessageFlowSinglePersistence;
    private final String clientId;
    private final MessageIDPools messageIDPools;
    private final ChannelPersistence channelPersistence;
    private static final ListeningScheduledExecutorService SCHEDULED_EXECUTOR_SERVICE = MoreExecutors.listeningDecorator(Executors.newSingleThreadScheduledExecutor());

    public SendOutMessagesFutureListener(
            @NotNull QueuedMessagesSinglePersistence queuedMessagesSinglePersistence,
            @NotNull OutgoingMessageFlowSinglePersistence outgoingMessageFlowSinglePersistence,
            @NotNull String clientId,
            @NotNull MessageIDPools messageIDPools,
            ChannelPersistence channelPersistence) {
        this.channelPersistence = channelPersistence;
        Preconditions.checkNotNull(queuedMessagesSinglePersistence, "Client Session Queued Message Persistence must not be null");
        Preconditions.checkNotNull(outgoingMessageFlowSinglePersistence, "Outgoing Message Flow Persistence must not be null");
        Preconditions.checkNotNull(clientId, "Client ID Persistence must not be null");
        Preconditions.checkNotNull(messageIDPools, "Message ID Pools must not be null");
        this.queuedMessagesSinglePersistence = queuedMessagesSinglePersistence;
        this.outgoingMessageFlowSinglePersistence = outgoingMessageFlowSinglePersistence;
        this.clientId = clientId;
        this.messageIDPools = messageIDPools;
    }

    @Override
    public void operationComplete(ChannelFuture future) throws Exception {
        if (!future.isSuccess()) {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("ConnAck for client {} could not be sent, skip sending queued messages for now", this.clientId);
            }
            return;
        }
        ChannelFuture channelFuture = b(future);
        if (channelFuture == null) {
            future.addListener(new a(this.queuedMessagesSinglePersistence, this.clientId, this.messageIDPools, this.channelPersistence));
            return;
        }
        future.channel().flush();
        channelFuture.addListener(new b(this.queuedMessagesSinglePersistence, this.clientId, this.messageIDPools, this.channelPersistence));
    }

    @com.hivemq.spi.annotations.Nullable
    private ChannelFuture b(ChannelFuture channelFuture) {
        ImmutableList<MessageWithId> outgoingMessages = this.outgoingMessageFlowSinglePersistence.getAll(this.clientId);
        ChannelFuture localChannelFuture = null;
        Iterator localIterator = outgoingMessages.iterator();
        while (localIterator.hasNext()) {
            MessageWithId localMessageWithId = (MessageWithId) localIterator.next();
            if (localMessageWithId instanceof Publish) {
                Publish localPublish = (Publish) localMessageWithId;
                if (localPublish.getQoS().getQosNumber() > 0) {
                    localChannelFuture = channelFuture.channel().write(localPublish);
                } else {
                    LOGGER.debug("QoS 0 found in inflight queue for client {}", this.clientId);
                }
            } else if (localMessageWithId instanceof PubRel || localMessageWithId instanceof PubRec) {
                localChannelFuture = channelFuture.channel().write(localMessageWithId);
            }
        }
        return localChannelFuture;
    }

    @VisibleForTesting
    static class b implements ChannelFutureListener {
        private static final Logger LOGGER = LoggerFactory.getLogger(b.class);
        private final QueuedMessagesSinglePersistence queuedMessagesSinglePersistence;
        private final String clientId;
        private final MessageIDPools messageIDPools;
        private final ChannelPersistence channelPersistence;

        b(@NotNull QueuedMessagesSinglePersistence queuedMessagesSinglePersistence,
          @NotNull String clientId,
          @NotNull MessageIDPools messageIDPools,
          ChannelPersistence channelPersistence) {
            this.channelPersistence = channelPersistence;
            Preconditions.checkNotNull(queuedMessagesSinglePersistence, "Client Session Queued Message Persistence must not be null");
            Preconditions.checkNotNull(clientId, "Client ID Persistence must not be null");
            Preconditions.checkNotNull(messageIDPools, "Message ID Pools must not be null");
            this.queuedMessagesSinglePersistence = queuedMessagesSinglePersistence;
            this.clientId = clientId;
            this.messageIDPools = messageIDPools;
        }

        @Override
        public void operationComplete(ChannelFuture future) throws Exception {
            if (!future.isSuccess()) {
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("Not able to send out stored inflight messages for client {}, skip sending queued messages for now", this.clientId);
                }
                return;
            }
            future.addListener(new a(this.queuedMessagesSinglePersistence, this.clientId, this.messageIDPools, this.channelPersistence));
        }
    }

    @VisibleForTesting
    static class a
            implements FutureCallback<Void>, ChannelFutureListener {
        private static final Logger LOGGER = LoggerFactory.getLogger(a.class);
        private final QueuedMessagesSinglePersistence queuedMessagesSinglePersistence;
        private final String clientId;
        private final MessageIDPools messageIDPools;
        private final ChannelPersistence channelPersistence;

        a(@NotNull QueuedMessagesSinglePersistence queuedMessagesSinglePersistence,
          @NotNull String clientId,
          @NotNull MessageIDPools messageIDPools,
          ChannelPersistence channelPersistence) {
            this.channelPersistence = channelPersistence;
            Preconditions.checkNotNull(queuedMessagesSinglePersistence, "Client Session Queued Message Persistence must not be null");
            Preconditions.checkNotNull(clientId, "Client ID Persistence must not be null");
            Preconditions.checkNotNull(messageIDPools, "Message ID Pools must not be null");
            this.queuedMessagesSinglePersistence = queuedMessagesSinglePersistence;
            this.clientId = clientId;
            this.messageIDPools = messageIDPools;
        }

        @Override
        public void onSuccess(@Nullable Void result) {
            Channel channel = this.channelPersistence.getChannel(this.clientId);
            a(channel);
        }

        public void onFailure(Throwable t) {
            Channel channel = this.channelPersistence.getChannel(this.clientId);
            retrySend(channel);
        }

        @Override
        public void operationComplete(ChannelFuture future) throws Exception {
            if (!future.isSuccess()) {
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("Not able to send out queued messages for client {}, skip sending other queued messages for now", this.clientId);
                }
                return;
            }
            a(future.channel());
        }

        private void a(Channel channel) {
            if (channel != null) {
                int messageId;
                try {
                    messageId = this.messageIDPools.forClient(this.clientId).next();
                } catch (ProduceMessageIdException e) {
                    retrySend(channel);
                    return;
                }
                a(channel, messageId);
            }
        }

        private void retrySend(Channel channel) {
            LOGGER.trace("No message id available for client {}, retrying sending of queued messages in 1 second", this.clientId);
            channel.eventLoop().schedule(() -> {
                a(channelPersistence.getChannel(this.clientId));
                return null;
            }, 250L, TimeUnit.MILLISECONDS);
        }

        private void a(Channel channel, int messageId) {
            ListenableFuture<InternalPublish> future = this.queuedMessagesSinglePersistence.poll(this.clientId);
            Futures.addCallback(future, new FutureCallback<InternalPublish>() {

                @Override
                public void onSuccess(@Nullable InternalPublish result) {
                    if (result != null) {
                        result.setMessageId(messageId);
                        channel.writeAndFlush(result).addListener(
                                new a(queuedMessagesSinglePersistence, clientId, messageIDPools, channelPersistence));
                    }
                }

                @Override
                public void onFailure(Throwable t) {
                    PluginExceptionUtils.logOrThrow("Unable to poll next queued message for client " + clientId + ".", t);
                    channel.disconnect();
                }
            }, channel.eventLoop());
        }
    }
}
