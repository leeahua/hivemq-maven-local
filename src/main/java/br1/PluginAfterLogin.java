package br1;

import com.hivemq.spi.annotations.Nullable;
import com.hivemq.spi.callback.exception.AuthenticationException;
import com.hivemq.spi.security.ClientData;

public class PluginAfterLogin {
    private final ClientData clientData;
    private final boolean accepted;
    private final AuthenticationException exception;

    public PluginAfterLogin(ClientData clientData,
                            boolean accepted,
                            @Nullable AuthenticationException exception) {
        this.clientData = clientData;
        this.accepted = accepted;
        this.exception = exception;
    }

    public ClientData getClientData() {
        return clientData;
    }

    public boolean isAccepted() {
        return accepted;
    }

    @Nullable
    public AuthenticationException getException() {
        return exception;
    }
}
