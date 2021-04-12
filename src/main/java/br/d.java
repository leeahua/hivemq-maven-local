package br;

import bu.MessageIDPools;
import bw.ProduceMessageIdException;
import cb1.PluginExceptionUtils;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.hivemq.spi.annotations.NotNull;
import com.hivemq.spi.message.QoS;
import com.hivemq.spi.message.Topic;
import i.ClusterIdProducer;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import x.RetainedMessagesSinglePersistence;

import javax.annotation.Nullable;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

// TODO:
public class d
        implements FutureCallback<Void> {
    private static final Logger LOGGER = LoggerFactory.getLogger(d.class);
    private final String lastTopic;
    private final Queue<String> topics;
    private final Channel channel;
    private final QoS qoS;
    private final MessageIDPools messageIDPools;
    private final RetainedMessagesSinglePersistence retainedMessagesSinglePersistence;
    private final ClusterIdProducer clusterIdProducer;

    public d(@NotNull String lastTopic,
             @NotNull Queue<String> topics,
             @NotNull Channel channel,
             @NotNull QoS qoS,
             @NotNull MessageIDPools messageIDPools,
             @NotNull RetainedMessagesSinglePersistence retainedMessagesSinglePersistence,
             ClusterIdProducer clusterIdProducer) {
        this.clusterIdProducer = clusterIdProducer;
        Preconditions.checkNotNull(lastTopic, "Last Topic must not be null");
        Preconditions.checkNotNull(topics, "Topics must not be null");
        Preconditions.checkNotNull(channel, "Channel must not be null");
        Preconditions.checkNotNull(qoS, "Qos must not be null");
        Preconditions.checkNotNull(messageIDPools, "MessageIDPools must not be null");
        Preconditions.checkNotNull(retainedMessagesSinglePersistence, "RetainedMessagesPersistence must not be null");
        this.lastTopic = lastTopic;
        this.topics = topics;
        this.channel = channel;
        this.qoS = qoS;
        this.messageIDPools = messageIDPools;
        this.retainedMessagesSinglePersistence = retainedMessagesSinglePersistence;
    }

    @Override
    public void onSuccess(@Nullable Void result) {
        a();
    }

    private void a() {
        if (!this.channel.isActive()) {
            return;
        }
        String topicName = this.topics.poll();
        if (topicName != null) {
            ListenableFuture future = a.a(new Topic(topicName, this.qoS), this.channel, this.messageIDPools, this.retainedMessagesSinglePersistence, this.clusterIdProducer);
            Futures.addCallback(future, new d(topicName, this.topics, this.channel, this.qoS, this.messageIDPools, this.retainedMessagesSinglePersistence, this.clusterIdProducer), this.channel.eventLoop());
        }
    }

    public void onFailure(Throwable t) {
        if (!(t instanceof ProduceMessageIdException)) {
            PluginExceptionUtils.logOrThrow("Unable to send retained message for topic " + this.lastTopic + ".", t);
            this.channel.disconnect();
            return;
        }
        if (!this.channel.isActive()) {
            return;
        }
        this.channel.eventLoop().schedule(() -> {
            ListenableFuture future = a.a(new Topic(lastTopic, qoS), channel, messageIDPools, retainedMessagesSinglePersistence, clusterIdProducer);
            Futures.addCallback(future, new d(lastTopic, topics, channel, qoS, messageIDPools, retainedMessagesSinglePersistence, clusterIdProducer), channel.eventLoop());
        }, 1L, TimeUnit.SECONDS);
    }
}
