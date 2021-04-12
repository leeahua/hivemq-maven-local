package bq1;

import com.hivemq.spi.security.ClientData;

public class PluginOnDisconnect {
    private final ClientData clientData;
    private final boolean receivedDisconnect;

    public PluginOnDisconnect(ClientData clientData, boolean receivedDisconnect) {
        this.clientData = clientData;
        this.receivedDisconnect = receivedDisconnect;
    }

    public ClientData getClientData() {
        return clientData;
    }

    public boolean isReceivedDisconnect() {
        return receivedDisconnect;
    }
}
