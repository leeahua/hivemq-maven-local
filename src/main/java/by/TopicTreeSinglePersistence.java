package by;

import ai.TopicSubscribers;
import com.google.common.util.concurrent.ListenableFuture;
import com.hivemq.spi.annotations.NotNull;
import com.hivemq.spi.annotations.Nullable;
import com.hivemq.spi.annotations.ThreadSafe;
import com.hivemq.spi.message.Topic;

public interface TopicTreeSinglePersistence {
    @ThreadSafe
    ListenableFuture<Void> addSubscription(@NotNull String subscriber, @NotNull Topic topic, byte shared, @Nullable String groupId);

    @ThreadSafe
    ListenableFuture<TopicSubscribers> getSubscribers(@NotNull String topic);

    @ThreadSafe
    ListenableFuture<Void> removeSubscription(@NotNull String subscriber, @NotNull String topic);
}
