package v;

public class ClientSession {
    private boolean connected;
    private boolean persistentSession;
    private String connectedNode;

    public ClientSession(boolean connected, boolean persistentSession, String connectedNode) {
        this.connected = connected;
        this.persistentSession = persistentSession;
        this.connectedNode = connectedNode;
    }

    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    public boolean isPersistentSession() {
        return persistentSession;
    }

    public void setPersistentSession(boolean persistentSession) {
        this.persistentSession = persistentSession;
    }

    public String getConnectedNode() {
        return connectedNode;
    }

    public void setConnectedNode(String connectedNode) {
        this.connectedNode = connectedNode;
    }
}
