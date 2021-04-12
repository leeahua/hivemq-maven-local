package bu1;

import com.hivemq.spi.message.Connect;
import com.hivemq.spi.security.ClientData;

public class PluginOnConnect {
    private final Connect connect;
    private final ClientData clientData;

    public PluginOnConnect(Connect connect, ClientData clientData) {
        this.connect = connect;
        this.clientData = clientData;
    }

    public Connect getConnect() {
        return connect;
    }

    public ClientData getClientData() {
        return clientData;
    }
}
