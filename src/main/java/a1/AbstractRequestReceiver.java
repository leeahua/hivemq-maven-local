package a1;

import ab.ClusterResponse;
import ab.ClusterResponseCode;
import i.ClusterIdProducer;
import q.ConsistentHashingRing;

import java.util.HashSet;
import java.util.Set;

public abstract class AbstractRequestReceiver {
    private final ConsistentHashingRing primaryRing;
    private final ConsistentHashingRing minorityRing;
    private final ClusterIdProducer clusterIdProducer;
    private final int replicateCount;

    protected AbstractRequestReceiver(ConsistentHashingRing primaryRing,
                                      ConsistentHashingRing minorityRing,
                                      ClusterIdProducer clusterIdProducer,
                                      int replicateCount) {
        this.primaryRing = primaryRing;
        this.minorityRing = minorityRing;
        this.clusterIdProducer = clusterIdProducer;
        this.replicateCount = replicateCount;
    }

    protected <T> boolean responsible(String key, ClusterResponse response, Class<T> resultType) {
        if (this.primaryRing.getNode(key).equals(this.clusterIdProducer.get())) {
            return true;
        }
        response.sendResult(ClusterResponseCode.NOT_RESPONSIBLE, null);
        return false;
    }

    protected <T> boolean writeable(String key, ClusterResponse response, Class<T> resultType) {
        Set<String> nodes = new HashSet<>();
        nodes.addAll(this.primaryRing.getReplicaNodes(key, this.replicateCount));
        nodes.addAll(this.minorityRing.getReplicaNodes(key, this.replicateCount));
        nodes.add(this.minorityRing.getNode(key));
        nodes.remove(this.primaryRing.getNode(key));
        if (nodes.contains(this.clusterIdProducer.get())) {
            return true;
        }
        T object = null;
        response.sendResult(ClusterResponseCode.NOT_RESPONSIBLE, object);
        return false;
    }

    protected <T> boolean readable(String key, ClusterResponse response, Class<T> resultType) {
        if (this.primaryRing.getNode(key).equals(this.clusterIdProducer.get())) {
            return true;
        }
        if (this.primaryRing.getReplicaNodes(key, this.replicateCount)
                .contains(this.clusterIdProducer.get())) {
            return true;
        }
        T object = null;
        response.sendResult(ClusterResponseCode.NOT_RESPONSIBLE, object);
        return false;
    }
}
