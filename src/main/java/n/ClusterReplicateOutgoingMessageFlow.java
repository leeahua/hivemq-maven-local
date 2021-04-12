package n;

public class ClusterReplicateOutgoingMessageFlow extends ClusterReplicate {
    private int replicationInterval;

    public int getReplicationInterval() {
        return replicationInterval;
    }

    public void setReplicationInterval(int replicationInterval) {
        this.replicationInterval = replicationInterval;
    }
}
