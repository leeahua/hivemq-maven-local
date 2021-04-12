package bi1;

public class ClientSessionSubscriptionKey {
    private final String clientId;
    private final Long timestamp;

    ClientSessionSubscriptionKey(String clientId, Long timestamp) {
        this.clientId = clientId;
        this.timestamp = timestamp;
    }

    public String getClientId() {
        return clientId;
    }

    public Long getTimestamp() {
        return timestamp;
    }
}
