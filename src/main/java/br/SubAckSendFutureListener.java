package br;

import bu.MessageIDPools;
import cb1.PluginExceptionUtils;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.hivemq.spi.annotations.NotNull;
import com.hivemq.spi.message.Topic;
import i.ClusterIdProducer;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import x.RetainedMessagesSinglePersistence;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
// TODO:
public class SubAckSendFutureListener implements ChannelFutureListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(SubAckSendFutureListener.class);
    private final List<Topic> subscriptions;
    private final RetainedMessagesSinglePersistence retainedMessagesSinglePersistence;
    private final MessageIDPools messageIDPools;
    private final ClusterIdProducer clusterIdProducer;

    public SubAckSendFutureListener(@NotNull List<Topic> subscriptions,
                                    @NotNull RetainedMessagesSinglePersistence retainedMessagesSinglePersistence,
                                    @NotNull MessageIDPools messageIDPools,
                                    @NotNull ClusterIdProducer clusterIdProducer) {
        this.clusterIdProducer = clusterIdProducer;
        Preconditions.checkNotNull(subscriptions, "Subscriptions must not be null");
        Preconditions.checkNotNull(retainedMessagesSinglePersistence, "RetainedMessagesPersistence must not be null");
        Preconditions.checkNotNull(messageIDPools, "MessageIdPools must not be null");
        Preconditions.checkNotNull(clusterIdProducer, "ClusterID must not be null");
        this.subscriptions = subscriptions;
        this.retainedMessagesSinglePersistence = retainedMessagesSinglePersistence;
        this.messageIDPools = messageIDPools;
    }

    @Override
    public void operationComplete(ChannelFuture future) throws Exception {
        if (!future.isSuccess()) {
            return;
        }
        Channel channel = future.channel();
        List localList = a(channel);
        if (!localList.isEmpty()) {
            a(localList, channel);
        }
    }

    private List<Topic> a(Channel channel) {
        List<Topic> withWildcards = new ArrayList<>(this.subscriptions.size());
        this.subscriptions.forEach(topic -> {
            String subscription = topic.getTopic();
            if(subscription.contains("#") || subscription.contains("+")){
                withWildcards.add(topic);
                return;
            }
            ListenableFuture localListenableFuture = LOGGER.a(topic, channel, this.messageIDPools, this.retainedMessagesSinglePersistence, this.clusterIdProducer);
            Futures.addCallback(localListenableFuture, new f(channel, topic, this.messageIDPools, this.retainedMessagesSinglePersistence, this.clusterIdProducer), channel.eventLoop());

        });

        Iterator localIterator = this.subscriptions.iterator();
        while (localIterator.hasNext()) {
            Topic localTopic = (Topic) localIterator.next();
            String str = localTopic.getTopic();
            if ((str.contains("#")) || (str.contains("+"))) {
                withWildcards.add(localTopic);
            } else {
                ListenableFuture localListenableFuture = LOGGER.a(localTopic, channel, this.messageIDPools, this.retainedMessagesSinglePersistence, this.clusterIdProducer);
                Futures.addCallback(localListenableFuture, new f(channel, localTopic, this.messageIDPools, this.retainedMessagesSinglePersistence, this.clusterIdProducer), channel.eventLoop());
            }
        }
        return withWildcards;
    }

    private void a(@NotNull List<Topic> paramList, @NotNull Channel paramChannel) {
        Iterator localIterator1 = paramList.iterator();
        while (localIterator1.hasNext()) {
            Topic localTopic = (Topic) localIterator1.next();
            ImmutableList localImmutableList = this.retainedMessagesSinglePersistence.getTopics(localTopic.getTopic());
            Iterator localIterator2 = localImmutableList.iterator();
            while (localIterator2.hasNext()) {
                ListenableFuture localListenableFuture = (ListenableFuture) localIterator2.next();
                Futures.addCallback(localListenableFuture, new a(localTopic, paramChannel, this.messageIDPools, this.retainedMessagesSinglePersistence, this.clusterIdProducer), paramChannel.eventLoop());
            }
        }
    }


    static class a
            implements FutureCallback<Set<String>> {
        public static final int a = 25;
        private final Topic topic;
        private final Channel channel;
        private final MessageIDPools messageIDPools;
        private final RetainedMessagesSinglePersistence retainedMessagesSinglePersistence;
        private final ClusterIdProducer clusterIdProducer;

        public a(Topic paramTopic, Channel paramChannel, MessageIDPools paramf, RetainedMessagesSinglePersistence parame, ClusterIdProducer paramc) {
            this.topic = paramTopic;
            this.channel = paramChannel;
            this.messageIDPools = paramf;
            this.retainedMessagesSinglePersistence = parame;
            this.clusterIdProducer = paramc;
        }

        public void a(Set<String> paramSet) {

        }

        @Override
        public void onSuccess(@Nullable Set<String> result) {
            if (result.size() == 0) {
                return;
            }
            ConcurrentLinkedQueue<String> localConcurrentLinkedQueue = new ConcurrentLinkedQueue<>(result);
            for (int i = 0; i < 25; i++) {
                String str = localConcurrentLinkedQueue.poll();
                if (str == null) {
                    return;
                }
                ListenableFuture future = br.a.a(new Topic(str, this.topic.getQoS()), this.channel, this.messageIDPools, this.retainedMessagesSinglePersistence, this.clusterIdProducer);
                Futures.addCallback(future, new d(str, localConcurrentLinkedQueue, this.channel, this.topic.getQoS(), this.messageIDPools, this.retainedMessagesSinglePersistence, this.clusterIdProducer), this.channel.eventLoop());
            }
        }

        public void onFailure(Throwable t) {
            PluginExceptionUtils.logOrThrow("Unable to send retained messages on topic " + this.topic.getTopic() + " to client " + this.clusterIdProducer + ".", t);
            this.channel.disconnect();
        }
    }
}
