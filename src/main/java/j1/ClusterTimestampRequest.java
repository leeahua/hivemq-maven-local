package j1;

public abstract class ClusterTimestampRequest implements ClusterKeyRequest {
    private final long timestamp;

    protected ClusterTimestampRequest(long timestamp) {
        this.timestamp = timestamp;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
