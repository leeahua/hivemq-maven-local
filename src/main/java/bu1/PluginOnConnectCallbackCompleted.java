package bu1;

import com.hivemq.spi.callback.events.OnConnectCallback;
import com.hivemq.spi.message.Connect;
import com.hivemq.spi.message.ReturnCode;
import com.hivemq.spi.security.ClientData;

import java.util.Queue;

public class PluginOnConnectCallbackCompleted {
    private final ClientData clientData;
    private final Connect connect;
    private final boolean refused;
    private final ReturnCode returnCode;
    private final Queue<OnConnectCallback> leftCallbacks;

    public PluginOnConnectCallbackCompleted(ClientData clientData,
                                            Connect connect,
                                            boolean refused,
                                            ReturnCode returnCode,
                                            Queue<OnConnectCallback> leftCallbacks) {
        this.clientData = clientData;
        this.connect = connect;
        this.refused = refused;
        this.returnCode = returnCode;
        this.leftCallbacks = leftCallbacks;
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

    public Queue<OnConnectCallback> getLeftCallbacks() {
        return leftCallbacks;
    }
}
