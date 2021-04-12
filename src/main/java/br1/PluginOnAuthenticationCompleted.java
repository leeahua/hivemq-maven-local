package br1;

import com.hivemq.spi.callback.exception.AuthenticationException;
import com.hivemq.spi.message.Connect;
import com.hivemq.spi.message.ReturnCode;
import com.hivemq.spi.security.ClientCredentials;

public class PluginOnAuthenticationCompleted {
    private final Connect connect;
    private final ClientCredentials clientCredentials;
    private final ReturnCode returnCode;
    private final AuthenticationException exception;

    public PluginOnAuthenticationCompleted(Connect connect,
                                           ClientCredentials clientCredentials,
                                           ReturnCode returnCode) {
        this.connect = connect;
        this.clientCredentials = clientCredentials;
        this.returnCode = returnCode;
        this.exception = null;
    }

    public PluginOnAuthenticationCompleted(Connect connect,
                                           ClientCredentials clientCredentials,
                                           ReturnCode returnCode,
                                           AuthenticationException exception) {
        this.connect = connect;
        this.clientCredentials = clientCredentials;
        this.returnCode = returnCode;
        this.exception = exception;
    }

    public Connect getConnect() {
        return connect;
    }

    public ClientCredentials getClientCredentials() {
        return clientCredentials;
    }

    public ReturnCode getReturnCode() {
        return returnCode;
    }

    public AuthenticationException getException() {
        return exception;
    }
}
