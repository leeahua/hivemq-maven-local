package p;

public class ClusterRequestException extends RuntimeException {
    public ClusterRequestException() {
    }

    public ClusterRequestException(String message) {
        super(message);
    }

    public ClusterRequestException(String message, Throwable cause) {
        super(message, cause);
    }

    public ClusterRequestException(Throwable cause) {
        super(cause);
    }
}
