package bs1;

import bu.InternalPublish;
import ci.AuthorizationResultImpl;
import com.hivemq.spi.callback.exception.AuthenticationException;
import com.hivemq.spi.callback.security.OnAuthorizationCallback;
import com.hivemq.spi.security.ClientData;
import io.netty.channel.ChannelHandlerContext;

import java.util.List;
import java.util.Queue;

public class PluginOnAuthorizationPublishCallbackCompleted {
    private final ChannelHandlerContext ctx;
    private final Queue<OnAuthorizationCallback> leftCallbacks;
    private final List<AuthorizationResultImpl> results;
    private final InternalPublish publish;
    private final ClientData clientData;
    private final AuthenticationException exception;

    public PluginOnAuthorizationPublishCallbackCompleted(
            ChannelHandlerContext ctx,
            Queue<OnAuthorizationCallback> leftCallbacks,
            List<AuthorizationResultImpl> results,
            InternalPublish publish,
            ClientData clientData,
            AuthenticationException exception) {
        this.ctx = ctx;
        this.leftCallbacks = leftCallbacks;
        this.results = results;
        this.publish = publish;
        this.clientData = clientData;
        this.exception = exception;
    }

    public ChannelHandlerContext getCtx() {
        return ctx;
    }

    public Queue<OnAuthorizationCallback> getLeftCallbacks() {
        return leftCallbacks;
    }

    public List<AuthorizationResultImpl> getResults() {
        return results;
    }

    public InternalPublish getPublish() {
        return publish;
    }

    public ClientData getClientData() {
        return clientData;
    }

    public AuthenticationException getException() {
        return exception;
    }

    public long getTimestamp() {
        return this.publish.getTimestamp();
    }
}
