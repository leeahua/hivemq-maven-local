package bn1;

import ak.VectorClock;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ListenableFuture;
import com.hivemq.spi.annotations.NotNull;
import com.hivemq.spi.message.MessageWithId;
import u.Filter;
import u1.OutgoingMessageFlowPutReplicateRequest;
import u1.OutgoingMessageFlowPutRequest;
import u1.OutgoingMessageFlowRemoveAllReplicateRequest;
import u1.OutgoingMessageFlowReplicateRequest;

public interface OutgoingMessageFlowClusterPersistence {
    ListenableFuture<Void> handlePutRequest(@NotNull OutgoingMessageFlowPutRequest request, @NotNull String connectedNode);

    ListenableFuture<Void> replicateClientFlow(String clientId, ImmutableList<MessageWithId> messages, VectorClock requestVectorClock);

    ListenableFuture<Void> handlePutReplica(OutgoingMessageFlowPutReplicateRequest request);

    ListenableFuture<Void> add(String clientId, MessageWithId message);

    ListenableFuture<Void> removeAll(@NotNull String clientId);

    ListenableFuture<Void> replicateRemoveAll(String clientId, VectorClock requestVectorClock);

    ListenableFuture<Void> handleRemoveAllReplica(OutgoingMessageFlowRemoveAllReplicateRequest request);

    ListenableFuture<Void> put(@NotNull String clientId);

    ListenableFuture<OutgoingMessageFlowReplicateRequest> getLocalData(@NotNull Filter filter);

    ListenableFuture<Void> removeLocally(Filter filter);

    ListenableFuture<Void> processReplicationRequest(@NotNull OutgoingMessageFlowReplicateRequest request);
}
