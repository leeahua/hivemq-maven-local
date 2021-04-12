package t1;

import j1.ClusterKeyRequest;

public class ClientConnectedRequest implements ClusterKeyRequest {
    private final boolean persistentSession;
    private final String clientId;
    private final String connectedNode;
    
    public ClientConnectedRequest(boolean persistentSession, String clientId, String connectedNode) {
        this.persistentSession = persistentSession;
        this.clientId = clientId;
        this.connectedNode = connectedNode;
    }

    public boolean isPersistentSession() {
        return persistentSession;
    }

    public String getClientId() {
        return clientId;
    }

    public String getConnectedNode() {
        return connectedNode;
    }

    public String getKey() {
        return this.clientId;
    }
}
