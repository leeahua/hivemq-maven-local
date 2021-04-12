package u;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Futures;
import com.hivemq.spi.annotations.NotNull;
import j1.ClusterRequestFuture;
import j1.ClusterKeyRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import q.ConsistentHashingRing;
import t.ClusterConnection;

import java.util.Set;

public abstract class AbstractClusterPersistence<S> {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractClusterPersistence.class);
    protected final ConsistentHashingRing primaryRing;
    protected final ClusterConnection clusterConnection;
    protected final Class<S> returnType;

    protected AbstractClusterPersistence(ConsistentHashingRing primaryRing,
                                         ClusterConnection clusterConnection,
                                         Class<S> returnType) {
        this.primaryRing = primaryRing;
        this.clusterConnection = clusterConnection;
        this.returnType = returnType;
    }

    protected <Q extends ClusterKeyRequest> ClusterRequestFuture<S, Q> send(@NotNull Q request) {
        Preconditions.checkNotNull(request, "Request must not be null");
        Set<String> persistNodes = persistNodes(request.getKey());
        if (persistNodes.contains(this.clusterConnection.getClusterId())) {
            S localResult = get(request.getKey());
            return new ClusterRequestFuture(Futures.immediateFuture(localResult), this.clusterConnection.getClusterId(), this.clusterConnection, request, 1000L, null);
        }
        String node = firstPersistNode(persistNodes);
        LOGGER.trace("Send {} GET to {}.", this.returnType.getSimpleName(), node);
        return this.clusterConnection.send(request, node, this.returnType);
    }


    protected Set<String> persistNodes(String key) {
        Set<String> replicaNodes = this.clusterConnection.getReplicaNodes(key, getReplicateCount());
        if (replicaNodes.isEmpty()) {
            return Sets.newHashSet(this.primaryRing.getNode(key));
        }
        replicaNodes.add(this.primaryRing.getNode(key));
        return replicaNodes;
    }

    @NotNull
    protected String originNode(@NotNull String key) {
        Preconditions.checkNotNull(key, "Key must not be null");
        String node = this.primaryRing.getNode(key);
        if (node == null) {
            return this.clusterConnection.getClusterId();
        }
        return node;
    }

    @NotNull
    protected <Q extends ClusterKeyRequest> ImmutableList<ClusterRequestFuture<Void, Q>> replicate(@NotNull Q request) {
        Preconditions.checkNotNull(request, "Request must not be null");
        Set<String> replicaNodes = this.clusterConnection.getReplicaNodes(request.getKey(), getReplicateCount());
        ImmutableList.Builder<ClusterRequestFuture<Void, Q>> builder = ImmutableList.builder();
        replicaNodes.forEach(node -> {
            LOGGER.trace("Replicate {} to {}", name(), node);
            builder.add(this.clusterConnection.send(request, node, Void.class));
        });
        return builder.build();
    }

    protected String firstPersistNode(Set<String> persistNodes) {
        return persistNodes.iterator().next();
    }

    protected abstract String name();

    protected abstract int getReplicateCount();

    protected abstract S get(String key);
}
