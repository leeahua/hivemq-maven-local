package by;

import bx.SubscriberWithQoS;
import com.google.common.collect.ImmutableSet;
import com.hivemq.spi.annotations.NotNull;
import com.hivemq.spi.annotations.Nullable;
import com.hivemq.spi.message.Topic;
import u.Filter;

import java.util.concurrent.CopyOnWriteArrayList;

public interface TopicTree {

    void addSubscription(@NotNull String subscriber, @NotNull Topic topic, byte shared, @Nullable String groupId);

    ImmutableSet<SubscriberWithQoS> getSubscribers(@NotNull String topic);

    ImmutableSet<SubscriberWithQoS> getSubscribers(@NotNull String topic, boolean excludeWildcard);

    void removeSubscriptions(@NotNull String subscriber);

    void removeSubscription(@NotNull String subscriber, @NotNull String topic);

    Segment get(@NotNull String segmentKey);

    void merge(@NotNull Segment segment, @NotNull String segmentKey);

    ImmutableSet<Segment> get(@NotNull Filter filter);

    void remove(@NotNull Filter filter);

    CopyOnWriteArrayList<SubscriberWithQoS> getWildcardSubscribers();
}
