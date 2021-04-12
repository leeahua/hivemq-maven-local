package bs1;

import bu.InternalPublish;
import com.hivemq.spi.callback.exception.AuthenticationException;
import com.hivemq.spi.callback.security.authorization.AuthorizationBehaviour;
import com.hivemq.spi.message.Connect;
import com.hivemq.spi.message.ReturnCode;
import com.hivemq.spi.message.Subscribe;
import com.hivemq.spi.security.ClientData;

import java.util.List;

public class PluginOnAuthorizationCompleted {
    private final Subscribe subscribe;
    private final InternalPublish publish;
    private final AuthorizationType type;
    private final ClientData clientData;
    private final AuthorizationBehaviour behaviour;
    private final List<AuthorizationBehaviour> behaviours;
    private final AuthenticationException exception;
    private final Connect connect;
    private final ReturnCode returnCode;
    private final boolean accepted;

    public PluginOnAuthorizationCompleted(Subscribe paramSubscribe,
                                          InternalPublish publish,
                                          AuthorizationType type,
                                          ClientData clientData,
                                          AuthorizationBehaviour behaviour,
                                          List<AuthorizationBehaviour> behaviours) {
        this(paramSubscribe, publish, type, clientData, behaviour, behaviours,
                null, null, null, true);
    }

    public PluginOnAuthorizationCompleted(Subscribe subscribe,
                                          InternalPublish publish,
                                          AuthorizationType type,
                                          ClientData clientData,
                                          AuthorizationBehaviour behaviour,
                                          List<AuthorizationBehaviour> behaviours,
                                          AuthenticationException exception,
                                          Connect connect,
                                          ReturnCode returnCode,
                                          boolean accepted) {
        this.subscribe = subscribe;
        this.publish = publish;
        this.type = type;
        this.clientData = clientData;
        this.behaviour = behaviour;
        this.behaviours = behaviours;
        this.exception = exception;
        this.connect = connect;
        this.returnCode = returnCode;
        this.accepted = accepted;
    }

    public Subscribe getSubscribe() {
        return subscribe;
    }

    public InternalPublish getPublish() {
        return publish;
    }

    public AuthorizationType getType() {
        return type;
    }

    public ClientData getClientData() {
        return clientData;
    }

    public AuthorizationBehaviour getBehaviour() {
        return behaviour;
    }

    public List<AuthorizationBehaviour> getBehaviours() {
        return behaviours;
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
