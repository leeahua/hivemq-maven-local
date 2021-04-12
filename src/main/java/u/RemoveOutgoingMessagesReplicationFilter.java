package u;

import bl1.ChannelPersistence;
import com.google.common.base.Preconditions;
import com.hivemq.spi.annotations.NotNull;
import q.ConsistentHashingRing;
import t.ClusterConnection;

import java.util.Set;

public class RemoveOutgoingMessagesReplicationFilter implements Filter {
    private final ClusterConnection clusterConnection;
    private final ConsistentHashingRing consistentHashingRing;
    private final int replicateCount;
    private final ChannelPersistence channelPersistence;

    public RemoveOutgoingMessagesReplicationFilter(
            ClusterConnection clusterConnection,
            ConsistentHashingRing consistentHashingRing,
            int replicateCount,
            ChannelPersistence channelPersistence) {
        this.clusterConnection = clusterConnection;
        this.consistentHashingRing = consistentHashingRing;
        this.replicateCount = replicateCount;
        this.channelPersistence = channelPersistence;
    }

    public boolean test(String key) {
        String clusterId = this.clusterConnection.getClusterId();
        String node = this.consistentHashingRing.getNode(key);
        Set<String> replicaNodes = getReplicaNodes(key, this.replicateCount);
        if (!clusterId.equals(node) &&
                !replicaNodes.contains(clusterId)) {
            return true;
        }
        return this.channelPersistence.getChannel(key) != null;
    }

    private Set<String> getReplicaNodes(@NotNull String key, int replicateCount) {
        Preconditions.checkNotNull(key, "Key must not be null");
        int viewCount = this.clusterConnection.getJChannel().getView().size();
        int replicas = replicateCount < viewCount - 1 ? replicateCount : viewCount - 1;
        return this.consistentHashingRing.getReplicaNodes(key, replicas);
    }
}
