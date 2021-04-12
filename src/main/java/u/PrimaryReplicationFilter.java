package u;

import com.google.common.base.Preconditions;
import com.hivemq.spi.annotations.NotNull;
import q.ConsistentHashingRing;
import t.ClusterConnection;

import java.util.Set;

public class PrimaryReplicationFilter implements Filter {
    private final ConsistentHashingRing primaryRing;
    private final ClusterConnection clusterConnection;
    private final String node;
    private final int replicateCount;

    public PrimaryReplicationFilter(ConsistentHashingRing primaryRing,
                                    ClusterConnection clusterConnection,
                                    String node,
                                    int replicateCount) {
        this.primaryRing = primaryRing;
        this.clusterConnection = clusterConnection;
        this.node = node;
        this.replicateCount = replicateCount;
    }

    public boolean test(String key) {
        String node = this.primaryRing.getNode(key);
        Set<String> replicaNodes = getReplicaNodes(key, this.replicateCount);
        replicaNodes.add(node);
        return replicaNodes.contains(this.node);
    }

    private Set<String> getReplicaNodes(@NotNull String key, int replicateCount) {
        Preconditions.checkNotNull(key, "Key must not be null");
        int viewCount = this.clusterConnection.getJChannel().getView().size();
        int count = replicateCount < viewCount - 1 ? replicateCount : viewCount - 1;
        return this.primaryRing.getReplicaNodes(key, count);
    }
}
