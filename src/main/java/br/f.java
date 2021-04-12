package br;

import bu.MessageIDPools;
import bw.ProduceMessageIdException;
import cb1.AttributeKeys;
import cb1.PluginExceptionUtils;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.hivemq.spi.message.Topic;
import i.ClusterIdProducer;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import x.RetainedMessagesSinglePersistence;

import java.util.concurrent.TimeUnit;

// TODO:
public class f
        implements FutureCallback<Void> {
    private static final Logger LOGGER = LoggerFactory.getLogger(f.class);
    private final Channel channel;
    private final Topic topic;
    private final MessageIDPools messageIDPools;
    private final RetainedMessagesSinglePersistence retainedMessagesSinglePersistence;
    private final ClusterIdProducer clusterIdProducer;

    public f(Channel channel,
             Topic topic,
             MessageIDPools messageIDPools,
             RetainedMessagesSinglePersistence retainedMessagesSinglePersistence,
             ClusterIdProducer clusterIdProducer) {
        this.channel = channel;
        this.topic = topic;
        this.messageIDPools = messageIDPools;
        this.retainedMessagesSinglePersistence = retainedMessagesSinglePersistence;
        this.clusterIdProducer = clusterIdProducer;
    }


    @Override
    public void onSuccess(@javax.annotation.Nullable Void result) {

    }

    @Override
    public void onFailure(Throwable t) {
        if (!(t instanceof ProduceMessageIdException)) {
            PluginExceptionUtils.logOrThrow("Unable to send retained messsage on topic " +
                    this.topic.getTopic() + " to client " +
                    this.channel.attr(AttributeKeys.MQTT_CLIENT_ID).get() + ".", t);
            this.channel.disconnect();
            return;
        }
        if (!this.channel.isActive()) {
            return;
        }
        this.channel.eventLoop().schedule(()->{
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Retrying retained message for client '{}' on topic '{}'.",
                        this.channel.attr(AttributeKeys.MQTT_CLIENT_ID).get(),
                        topic.getTopic());
            }
            ListenableFuture future = a.a(topic, channel, messageIDPools, retainedMessagesSinglePersistence, clusterIdProducer);
            Futures.addCallback(future, new f(channel, topic, messageIDPools, retainedMessagesSinglePersistence, clusterIdProducer), channel.eventLoop());
        }, 1L, TimeUnit.SECONDS);
    }
}
