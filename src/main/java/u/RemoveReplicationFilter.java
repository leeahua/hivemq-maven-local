package u;

import com.google.common.base.Preconditions;
import com.hivemq.spi.annotations.NotNull;
import q.ConsistentHashingRing;
import t.ClusterConnection;

import java.util.Set;

public class RemoveReplicationFilter implements Filter {
    private final ClusterConnection clusterConnection;
    private final ConsistentHashingRing ring;
    private final int replicateCount;

    public RemoveReplicationFilter(ClusterConnection clusterConnection,
                                   ConsistentHashingRing ring,
                                   int replicateCount) {
        this.clusterConnection = clusterConnection;
        this.ring = ring;
        this.replicateCount = replicateCount;
    }

    public boolean test(String key) {
        String clusterId = this.clusterConnection.getClusterId();
        String node = this.ring.getNode(key);
        Set<String> replicaNodes = getReplicaNodes(key, this.replicateCount);
        return !clusterId.equals(node) &&
                !replicaNodes.contains(clusterId);
    }

    private Set<String> getReplicaNodes(@NotNull String clientId, int replicateCount) {
        Preconditions.checkNotNull(clientId, "Key must not be null");
        int viewCount = this.clusterConnection.getJChannel().getView().size();
        int replicas = replicateCount < viewCount - 1 ? replicateCount : viewCount - 1;
        return this.ring.getReplicaNodes(clientId, replicas);
    }
}
