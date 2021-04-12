package y;

import ak.VectorClock;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.ListenableFuture;
import com.hivemq.spi.annotations.NotNull;
import com.hivemq.spi.annotations.Nullable;
import com.hivemq.spi.message.Topic;
import u.Filter;
import z1.ClientSessionSubscriptionReplicateRequest;

public interface ClientSessionSubscriptionsClusterPersistence {
    ClientSubscriptions getClientSubscriptions(String clientId);

    ListenableFuture<Void> replicateAdd(long requestTimestamp, @NotNull VectorClock requestVectorClock, @NotNull String clientId, @NotNull Topic topic);

    ListenableFuture<Void> replicateRemove(long requestTimestamp, @NotNull VectorClock requestVectorClock, @NotNull String clientId, @NotNull Topic topic);

    ListenableFuture<Void> replicateRemoveAll(long requestTimestamp, @NotNull VectorClock requestVectorClock, @NotNull String clientId);

    ListenableFuture<Void> removeAllLocally(@NotNull String clientId);

    ListenableFuture<Void> add(@NotNull String clientId, @Nullable Topic topic, @NotNull VectorClock requestVectorClock, long requestTimestamp);

    ListenableFuture<Void> remove(@NotNull String clientId, @Nullable Topic topic, @NotNull VectorClock requestVectorClock, long requestTimestamp);

    ListenableFuture<Void> removeAll(@NotNull String clientId, @NotNull VectorClock requestVectorClock, long requestTimestamp);

    ListenableFuture<Void> handleReplica(@NotNull String clientId, @NotNull ImmutableSet<Topic> subscriptions, @NotNull VectorClock requestVectorClock, long requestTimestamp);

    ListenableFuture<ImmutableSet<ClientSessionSubscriptionReplicateRequest>> getDataForReplica(@NotNull Filter filter);

    ListenableFuture<Void> removeLocally(@NotNull Filter filter);

    ListenableFuture<Void> cleanUp(long tombstoneMaxAge);
}
