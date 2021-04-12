package p;

public class ClusterRequestFailedException extends ClusterException {
    public ClusterRequestFailedException() {
    }

    public ClusterRequestFailedException(String message) {
        super(message);
    }
}
