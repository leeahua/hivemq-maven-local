package u;

import com.google.common.base.Preconditions;
import com.hivemq.spi.annotations.NotNull;
import org.jgroups.JChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Set;
import t.ClusterConnection;
import q.ConsistentHashingRing;
// TODO:
public class MinorityReplicationFilter implements Filter {
    private static final Logger LOGGER = LoggerFactory.getLogger(MinorityReplicationFilter.class);
    private final ConsistentHashingRing minorityRing;
    private final ClusterConnection clusterConnection;
    private final String node;
    private final int replicateCount;
    private final boolean f;
    private final boolean g;
    
    public MinorityReplicationFilter(ConsistentHashingRing minorityRing,
             ClusterConnection clusterConnection,
             String node,
             int replicateCount,
             boolean paramBoolean1,
             boolean paramBoolean2) {
        this.minorityRing = minorityRing;
        this.clusterConnection = clusterConnection;
        this.node = node;
        this.replicateCount = replicateCount;
        this.f = paramBoolean1;
        this.g = paramBoolean2;
    }

    public boolean test(String key) {
        String node = this.minorityRing.getNode(key);
        Set<String> replicaNodes = getReplicaNodes(key, this.replicateCount);
        if (this.f && node.equals(this.node)) {
            String str2 = this.minorityRing.getReplicaNodes(key, 1).iterator().next();
            if (str2.equals(this.clusterConnection.getClusterId())) {
                return true;
            }
        }
        return this.g &&
                node.equals(this.clusterConnection.getClusterId()) &&
                replicaNodes.contains(this.node);
    }


    private Set<String> getReplicaNodes(@NotNull String key, int replicateCount) {
        Preconditions.checkNotNull(key, "Key must not be null");
        JChannel jChannel = this.clusterConnection.getJChannel();
        if (!jChannel.isOpen() || !jChannel.isConnected() || jChannel.isClosed()) {
            return Collections.emptySet();
        }
        int viewCount = jChannel.getView().size();
        int replicas = replicateCount < viewCount - 1 ? replicateCount : viewCount - 1;
        return this.minorityRing.getReplicaNodes(key, replicas);
    }
}
