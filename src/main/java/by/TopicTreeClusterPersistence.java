package by;

import ai.TopicSubscribers;
import bx.SubscriberWithQoS;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.ListenableFuture;
import com.hivemq.spi.annotations.NotNull;
import com.hivemq.spi.annotations.Nullable;
import com.hivemq.spi.message.Topic;
import u.Filter;

import java.util.Set;

public interface TopicTreeClusterPersistence {
    void addTopicReplica(@NotNull String subscriber, @NotNull Topic topic, @NotNull String segmentKey, byte shared, @Nullable String groupId);

    void removeTopicReplica(@NotNull String subscriber, @NotNull String topic, @NotNull String segmentKey);

    ListenableFuture<Void> replicateAdd(@NotNull String subscriber, @NotNull Topic topic, @NotNull String segmentKey, byte shared, @Nullable String groupId);

    ListenableFuture<Void> replicateRemove(@NotNull String subscriber, @NotNull String topic, @NotNull String segmentKey);

    TopicSubscribers getLocally(@NotNull String segmentKey, @NotNull String topic);

    void mergeSegment(@NotNull Segment segment, @NotNull String segmentKey);

    ImmutableSet<Segment> getLocalDate(@NotNull Filter filter);

    void removeLocally(@NotNull Filter filter);

    Set<SubscriberWithQoS> getLocalSubscribersWithoutWildcard(@NotNull String topic);

    ImmutableSet<SubscriberWithQoS> getLocalSubscribers(@NotNull String topic);

    void subscribe(String subscriber, Topic subscription, byte shared, String groupId);
}
