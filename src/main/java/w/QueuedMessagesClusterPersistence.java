package w;

import ak.VectorClock;
import bu.InternalPublish;
import com.google.common.util.concurrent.ListenableFuture;
import com.hivemq.spi.annotations.NotNull;
import u.Filter;
import v1.MessageQueueReplicateRequest;
// TODO:
public interface QueuedMessagesClusterPersistence {
    boolean isEmpty(@NotNull String clientId);

    ListenableFuture<Void> replicateOffer(@NotNull String clientId, @NotNull InternalPublish publish, long requestTimestamp, @NotNull VectorClock requestVectorClock);

    ListenableFuture<Void> processOfferReplica(@NotNull String clientId, @NotNull InternalPublish publish, long requestTimestamp, @NotNull VectorClock requestVectorClock);

    ListenableFuture<InternalPublish> processPollRequest(@NotNull String clientId, @NotNull String requestId, @NotNull String sender);

    boolean b(@NotNull String clientId);

    ListenableFuture<Void> processRemoveReplica(@NotNull String clientId, long entryTimestamp, @NotNull String entryId, @NotNull VectorClock requestVectorClock, long requestTimestamp);

    ListenableFuture<Void> replicateRemove(@NotNull String clientId, long entryTimestamp, @NotNull String entryId, @NotNull VectorClock requestVectorClock, long requestTimestamp);

    ListenableFuture<Void> processRemoveAllReplica(@NotNull String clientId, long requestTimestamp, @NotNull VectorClock requestVectorClock);

    ListenableFuture<Void> replicateRemoveAll(String clientId, VectorClock requestVectorClock, long requestTimestamp);

    ListenableFuture<Void> remove(@NotNull String clientId);

    void processAckDrained(@NotNull String clientId, @NotNull String sender);

    ListenableFuture<MessageQueueReplicateRequest> getDataForReplica(@NotNull Filter filter);

    ListenableFuture<Void> processReplicationRequest(@NotNull MessageQueueReplicateRequest request);

    ListenableFuture<Void> ackDrained(@NotNull String clientId);

    void removeLocally(@NotNull Filter filter);

    ListenableFuture<Void> cleanUp(long tombstoneMaxAge);

    void resetQueueDrained(@NotNull String clientId);
}
