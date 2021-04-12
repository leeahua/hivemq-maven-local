package x;

import ak.VectorClock;
import bz.RetainedMessage;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.ListenableFuture;
import com.hivemq.spi.annotations.NotNull;
import com.hivemq.spi.annotations.Nullable;
import j1.ClusterReplicateRequest;
import u.Filter;

import java.util.Set;

public interface RetainedMessagesClusterPersistence {
    ListenableFuture<Void> remove(@NotNull String topic, @NotNull VectorClock requestVectorClock, long requestTimestamp);

    RetainedMessage getLocally(@NotNull String topic);

    ImmutableSet<ListenableFuture<Set<String>>> getLocalWithWildcards(@NotNull String topic);

    ListenableFuture<Void> persist(@NotNull String topic, @Nullable RetainedMessage retainedMessage, @NotNull VectorClock requestVectorClock, long requestTimestamp);

    ListenableFuture<ImmutableSet<ClusterReplicateRequest>> getDataForReplica(@NotNull Filter filter);

    ListenableFuture<Void> removeLocally(@NotNull Filter filter);

    ListenableFuture<Void> cleanUp(long tombstoneMaxAge);

    ListenableFuture<Void> replicateRemove(long requestTimestamp, @NotNull VectorClock requestVectorClock, @NotNull String topic);

    ListenableFuture<Void> replicateAdd(long requestTimestamp, @NotNull VectorClock requestVectorClock, @NotNull String topic, @NotNull RetainedMessage retainedMessage);

    ListenableFuture<Void> clear();

    ListenableFuture<Void> clearLocally();
}
