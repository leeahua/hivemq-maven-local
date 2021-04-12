package ch;

import com.hivemq.spi.annotations.NotNull;
import com.hivemq.spi.annotations.Nullable;
import com.hivemq.spi.callback.exception.AuthenticationException;
import com.hivemq.spi.message.ReturnCode;

public class PluginOnAuthenticationResult {
    private final boolean authenticated;
    private final ReturnCode returnCode;
    private final boolean refused;
    private final AuthenticationException exception;

    PluginOnAuthenticationResult(boolean authenticated,
                                 @NotNull ReturnCode returnCode,
                                 boolean refused,
                                 AuthenticationException exception) {
        this.authenticated = authenticated;
        this.returnCode = returnCode;
        this.refused = refused;
        this.exception = exception;
    }

    PluginOnAuthenticationResult(boolean authenticated,
                                 @NotNull ReturnCode returnCode,
                                 boolean refused) {
        this.authenticated = authenticated;
        this.returnCode = returnCode;
        this.refused = refused;
        this.exception = null;
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    public ReturnCode getReturnCode() {
        return returnCode;
    }

    public boolean isRefused() {
        return refused;
    }

    @Nullable
    public AuthenticationException getException() {
        return exception;
    }
}