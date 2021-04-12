package bs1;

import bu.InternalPublish;
import com.hivemq.spi.callback.exception.AuthenticationException;
import com.hivemq.spi.message.Connect;
import com.hivemq.spi.message.ReturnCode;
import com.hivemq.spi.message.Subscribe;
import com.hivemq.spi.security.ClientData;

public class PluginOnAuthorization {
    private final AuthorizationType type;
    private final Subscribe subscribe;
    private final ClientData clientData;
    private final InternalPublish publish;
    private final AuthenticationException exception;
    private final Connect connect;
    private final ReturnCode returnCode;
    private final boolean accepted;

    public PluginOnAuthorization(Subscribe subscribe,
                                 ClientData clientData) {
        this.subscribe = subscribe;
        this.clientData = clientData;
        this.publish = null;
        this.type = AuthorizationType.SUBSCRIBE;
        this.exception = null;
        this.connect = null;
        this.returnCode = null;
        this.accepted = true;
    }

    public PluginOnAuthorization(AuthorizationType type,
                                 InternalPublish publish,
                                 ClientData clientData) {
        this.subscribe = null;
        this.clientData = clientData;
        this.publish = publish;
        this.type = type;
        this.exception = null;
        this.connect = null;
        this.returnCode = null;
        this.accepted = true;
    }

    public PluginOnAuthorization(AuthorizationType type,
                                 InternalPublish publish,
                                 ClientData clientData,
                                 AuthenticationException exception,
                                 Connect connect,
                                 ReturnCode returnCode,
                                 boolean accepted) {
        this.subscribe = null;
        this.clientData = clientData;
        this.publish = publish;
        this.type = type;
        this.exception = exception;
        this.connect = connect;
        this.returnCode = returnCode;
        this.accepted = accepted;
    }

    public AuthorizationType getType() {
        return type;
    }

    public Subscribe getSubscribe() {
        return subscribe;
    }

    public ClientData getClientData() {
        return clientData;
    }

    public InternalPublish getPublish() {
        return publish;
    }

    public AuthenticationException getException() {
        return exception;
    }

    public Connect getConnect() {
        return connect;
    }

    public ReturnCode getReturnCode() {
        return returnCode;
    }

    public boolean isAccepted() {
        return accepted;
    }
}
