package bu1;

import com.hivemq.spi.message.Connect;
import com.hivemq.spi.message.ReturnCode;
import com.hivemq.spi.security.ClientData;

public class PluginOnConnectCompleted {
    private final ClientData clientData;
    private final Connect connect;
    private final boolean refused;
    private final ReturnCode returnCode;

    public PluginOnConnectCompleted(ClientData clientData,
                                    Connect connect,
                                    boolean refused,
                                    ReturnCode returnCode) {
        this.clientData = clientData;
        this.connect = connect;
        this.refused = refused;
        this.returnCode = returnCode;
    }

    public ClientData getClientData() {
        return clientData;
    }

    public Connect getConnect() {
        return connect;
    }

    public boolean isRefused() {
        return refused;
    }

    public ReturnCode getReturnCode() {
        return returnCode;
    }
}
