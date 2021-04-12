package bo;

public class ClusterPublishResult {
    private final SendStatus status;
    private final String clientId;

    public ClusterPublishResult(SendStatus status, String clientId) {
        this.status = status;
        this.clientId = clientId;
    }

    public SendStatus getStatus() {
        return status;
    }

    public String getClientId() {
        return clientId;
    }
}
