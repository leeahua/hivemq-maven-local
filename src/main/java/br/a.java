package br;

import bu.InternalPublish;
import bu.MessageIDPools;
import bw.ProduceMessageIdException;
import bz.RetainedMessage;
import cb1.AttributeKeys;
import cb1.QoSUtils;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.hivemq.spi.annotations.NotNull;
import com.hivemq.spi.annotations.Nullable;
import com.hivemq.spi.message.QoS;
import com.hivemq.spi.message.Topic;
import i.ClusterIdProducer;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import x.RetainedMessagesSinglePersistence;

// TODO:
class a {
    private static final Logger LOGGER = LoggerFactory.getLogger(a.class);

    public static ListenableFuture<Void> a(@Nullable Topic topic,
                                           @NotNull Channel channel,
                                           @NotNull MessageIDPools messageIDPools,
                                           @NotNull RetainedMessagesSinglePersistence retainedMessagesSinglePersistence,
                                           @NotNull ClusterIdProducer clusterIdProducer) {
        if (topic == null) {
            return Futures.immediateFuture(null);
        }
        String clientId = channel.attr(AttributeKeys.MQTT_CLIENT_ID).get();
        ListenableFuture<RetainedMessage> future = retainedMessagesSinglePersistence.getWithoutWildcards(topic.getTopic());
        SettableFuture<Void> settableFuture = SettableFuture.create();
        Futures.addCallback(future, new Callback(topic, clusterIdProducer,
                messageIDPools, clientId, settableFuture, channel), channel.eventLoop());
        return settableFuture;
    }


    private static final class Callback implements FutureCallback<RetainedMessage> {
        private Topic topic;
        private ClusterIdProducer clusterIdProducer;
        private MessageIDPools messageIDPools;
        private String clientId;
        private SettableFuture<Void> settableFuture;
        private Channel channel;

        public Callback(Topic topic,
                        ClusterIdProducer clusterIdProducer,
                        MessageIDPools messageIDPools,
                        String clientId,
                        SettableFuture<Void> settableFuture,
                        Channel channel) {
            this.topic = topic;
            this.clusterIdProducer = clusterIdProducer;
            this.messageIDPools = messageIDPools;
            this.clientId = clientId;
            this.settableFuture = settableFuture;
            this.channel = channel;
        }

        @Override
        public void onSuccess(@javax.annotation.Nullable RetainedMessage result) {
            if (result == null) {
                return;
            }
            QoS qoS = QoSUtils.min(topic.getQoS(), result.getQoS());
            InternalPublish publish = new InternalPublish(clusterIdProducer.get());
            publish.setPayload(result.getMessage());
            publish.setTopic(topic.getTopic());
            publish.setDuplicateDelivery(false);
            publish.setRetain(true);
            publish.setQoS(qoS);
            if (qoS.getQosNumber() > 0) {
                try {
                    int messageId = messageIDPools.forClient(clientId).next();
                    publish.setMessageId(messageId);
                    a(publish);
                } catch (ProduceMessageIdException e) {
                    settableFuture.setException(e);
                }
            } else {
                a(publish);
            }
        }

        private void a(InternalPublish publish) {
            LOGGER.trace("Sending retained message with topic [{}] for client [{}]",
                    topic, clientId);
            ChannelFuture channelFuture = channel.writeAndFlush(publish);
            channelFuture.addListener(future->{
                if (future.isSuccess()) {
                    settableFuture.set(null);
                } else {
                    settableFuture.setException(future.cause());
                }
            });
        }

        @Override
        public void onFailure(Throwable t) {
            settableFuture.setException(t);
        }
    }
}
