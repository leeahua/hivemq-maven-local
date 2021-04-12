package ai;

import bx.SubscriberWithQoS;
import by.TopicTreeClusterPersistence;
import by.TopicTree;
import by.TopicTreeImpl;
import by.TopicTreeSinglePersistence;
import by.Segment;
import u.NotImplementedException;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.hivemq.spi.annotations.NotNull;
import com.hivemq.spi.annotations.Nullable;
import com.hivemq.spi.message.Topic;
import d.CacheScoped;
import u.Filter;

import java.util.Set;

@CacheScoped
public class TopicTreeSinglePersistenceImpl
        implements TopicTreeClusterPersistence, TopicTreeSinglePersistence {
    private final TopicTree topicTree = new TopicTreeImpl(16, 16);
    private final TopicTree sysTopicTree = new TopicTreeImpl(16, 16);

    public ListenableFuture<Void> addSubscription(@NotNull String subscriber,
                                                  @NotNull Topic topic,
                                                  byte shared,
                                                  @Nullable String groupId) {
        Preconditions.checkNotNull(subscriber, "Subscriber must not be null");
        Preconditions.checkNotNull(topic, "Topic must not be null");
        if (topic.getTopic().startsWith("$SYS")) {
            this.sysTopicTree.addSubscription(subscriber, topic, shared, groupId);
            return Futures.immediateFuture(null);
        }
        this.topicTree.addSubscription(subscriber, topic, shared, groupId);
        return Futures.immediateFuture(null);
    }

    public void subscribe(String subscriber, Topic subscription, byte shared, String groupId) {
        if (subscription.getTopic().startsWith("$SYS")) {
            this.sysTopicTree.addSubscription(subscriber, subscription, shared, groupId);
        } else {
            this.topicTree.addSubscription(subscriber, subscription, shared, groupId);
        }
    }

    public ListenableFuture<TopicSubscribers> getSubscribers(@NotNull String topic) {
        Preconditions.checkNotNull(topic, "Topic must not be null");
        if (topic.startsWith("$SYS")) {
            return Futures.immediateFuture(new TopicSubscribers(this.sysTopicTree.getSubscribers(topic)));
        }
        SettableFuture<TopicSubscribers> settableFuture = SettableFuture.create();
        ImmutableSet.Builder<SubscriberWithQoS> builder = ImmutableSet.builder();
        builder.addAll(this.topicTree.getWildcardSubscribers());
        builder.addAll(this.topicTree.getSubscribers(topic));
        settableFuture.set(new TopicSubscribers(builder.build()));
        return settableFuture;
    }

    public ImmutableSet<SubscriberWithQoS> getLocalSubscribers(@NotNull String topic) {
        Preconditions.checkNotNull(topic, "Topic must not be null");
        if (topic.startsWith("$SYS")) {
            return this.sysTopicTree.getSubscribers(topic);
        }
        ImmutableSet.Builder<SubscriberWithQoS> builder = ImmutableSet.builder();
        builder.addAll(this.topicTree.getWildcardSubscribers());
        builder.addAll(this.topicTree.getSubscribers(topic));
        return builder.build();
    }

    public ListenableFuture<Void> removeSubscription(@NotNull String subscriber, @NotNull String topic) {
        Preconditions.checkNotNull(subscriber, "Subscriber must not be null");
        Preconditions.checkNotNull(topic, "Topic must not be null");
        if (topic.startsWith("$SYS")) {
            this.sysTopicTree.removeSubscription(subscriber, topic);
            return Futures.immediateFuture(null);
        }
        this.topicTree.removeSubscription(subscriber, topic);
        return Futures.immediateFuture(null);
    }

    public void addTopicReplica(@NotNull String subscriber, @NotNull Topic topic, @NotNull String segmentKey, byte shared, @Nullable String groupId) {
        throw new NotImplementedException("Cluster persistence method 'addTopicReplica' not implemented in single instance persistence");
    }

    public void removeTopicReplica(@NotNull String subscriber, @NotNull String topic, @NotNull String segmentKey) {
        throw new NotImplementedException("Cluster persistence method 'removeTopicReplica' not implemented in single instance persistence");
    }

    public ListenableFuture<Void> replicateAdd(@NotNull String subscriber, @NotNull Topic topic, @NotNull String segmentKey, byte shared, @Nullable String groupId) {
        throw new NotImplementedException("Cluster persistence method 'replicateAdd' not implemented in single instance persistence");
    }

    public ListenableFuture<Void> replicateRemove(@NotNull String subscriber, @NotNull String topic, @NotNull String segmentKey) {
        throw new NotImplementedException("Cluster persistence method 'replicateRemove' not implemented in single instance persistence");
    }

    public TopicSubscribers getLocally(@NotNull String segmentKey, @NotNull String topic) {
        throw new NotImplementedException("Cluster persistence method 'getLocally' not implemented in single instance persistence");
    }

    public void mergeSegment(@NotNull Segment segment, @NotNull String segmentKey) {
        throw new NotImplementedException("Cluster persistence method 'mergeSegment' not implemented in single instance persistence");
    }

    public ImmutableSet<Segment> getLocalDate(@NotNull Filter filter) {
        throw new NotImplementedException("Cluster persistence method 'getLocalDate' not implemented in single instance persistence");
    }

    public void removeLocally(@NotNull Filter filter) {
        throw new NotImplementedException("Cluster persistence method 'removeLocally' not implemented in single instance persistence");
    }

    public Set<SubscriberWithQoS> getLocalSubscribersWithoutWildcard(@NotNull String topic) {
        throw new NotImplementedException("Cluster persistence method 'getLocalSubscribersWithoutWildcard' not implemented in single instance persistence");
    }
}
