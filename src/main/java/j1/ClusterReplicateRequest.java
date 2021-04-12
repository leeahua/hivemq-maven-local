package j1;

import ak.VectorClock;

public abstract class ClusterReplicateRequest extends ClusterTimestampRequest {
    private final VectorClock vectorClock;

    protected ClusterReplicateRequest(long timestamp, VectorClock vectorClock) {
        super(timestamp);
        this.vectorClock = vectorClock;
    }

    public VectorClock getVectorClock() {
        return vectorClock;
    }
}
