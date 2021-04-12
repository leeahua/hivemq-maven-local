package bs1;

import bu.InternalPublish;
import ci.AuthorizationResultImpl;
import com.hivemq.spi.callback.exception.AuthenticationException;
import com.hivemq.spi.callback.security.OnAuthorizationCallback;
import com.hivemq.spi.message.Connect;
import com.hivemq.spi.message.ReturnCode;
import com.hivemq.spi.message.Subscribe;
import com.hivemq.spi.security.ClientData;

import java.util.List;
import java.util.Queue;

public class PluginOnAuthorizationCallbackCompleted {
    private final Queue<OnAuthorizationCallback> leftCallbacks;
    private final List<AuthorizationResultImpl> results;
    private final Subscribe subscribe;
    private final InternalPublish publish;
    private final ClientData clientData;
    private final AuthorizationType type;
    private final AuthenticationException exception;
    private final Connect connect;
    private final ReturnCode returnCode;
    private final boolean accepted;

    public PluginOnAuthorizationCallbackCompleted(
            Queue<OnAuthorizationCallback> leftCallbacks,
            List<AuthorizationResultImpl> results,
            Subscribe subscribe,
            InternalPublish publish,
            ClientData clientData,
            AuthorizationType type,
            AuthenticationException exception,
            Connect connect,
            ReturnCode returnCode,
            boolean accepted) {
        this.leftCallbacks = leftCallbacks;
        this.results = results;
        this.subscribe = subscribe;
        this.publish = publish;
        this.clientData = clientData;
        this.type = type;
        this.exception = exception;
        this.connect = connect;
        this.returnCode = returnCode;
        this.accepted = accepted;
    }

    public Queue<OnAuthorizationCallback> getLeftCallbacks() {
        return leftCallbacks;
    }

    public List<AuthorizationResultImpl> getResults() {
        return results;
    }

    public Subscribe getSubscribe() {
        return subscribe;
    }

    public InternalPublish getPublish() {
        return publish;
    }

    public ClientData getClientData() {
        return clientData;
    }

    public AuthorizationType getType() {
        return type;
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
