package v;

import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.ListenableFuture;
import com.hivemq.spi.annotations.NotNull;
import t1.ClientSessionReplicateRequest;
import u.Filter;
import u1.MessageFlow;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import ak.VectorClock;

public interface ClientSessionClusterPersistence {
    ListenableFuture<ClientSession> requestSession(@NotNull String clientId);

    ListenableFuture<ClientSession> getLocally(@NotNull String clientId);

    ListenableFuture<MessageFlow> clientConnectedRequest(@NotNull String clientId, boolean persistentSession, @NotNull String connectedNode);

    ListenableFuture<Void> clientDisconnectedRequest(@NotNull String clientId, @NotNull String connectedNode);

    ListenableFuture<Void> replicateClientSession(@NotNull String paramString, @NotNull ClientSession clientSession, long requestTimestamp, VectorClock requestVectorClock);

    ListenableFuture<ImmutableSet<ClientSessionReplicateRequest>> getDataForReplica(@NotNull Filter filter);

    ListenableFuture<Void> disconnectAllClients(String node);

    ListenableFuture<Void> removeLocally(@NotNull Filter filter);

    ListenableFuture<Void> cleanUp(long tombstoneMaxAge);

    ListenableFuture<Void> handleReplica(@NotNull String clientId, @NotNull ClientSession clientSession, long requestTimestamp, VectorClock requestVectorClock);

    ListenableFuture<Void> handleMergeReplica(@NotNull String clientId, @NotNull ClientSession clientSession, long timestamp);

    ListenableFuture<String> handleNodeForPublishRequest(@NotNull String clientId);

    ListenableFuture<String> nodeForPublish(@NotNull String clientId, ExecutorService executorService);

    ListenableFuture<Set<String>> getLocalDisconnectedClients();

    ListenableFuture<Set<String>> getLocalAllClients();

    ListenableFuture<Set<String>> getAllClients();
}
