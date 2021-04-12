package bs1;

import bu.InternalPublish;
import com.hivemq.spi.callback.exception.AuthenticationException;
import com.hivemq.spi.callback.security.authorization.AuthorizationBehaviour;
import com.hivemq.spi.security.ClientData;
import io.netty.channel.ChannelHandlerContext;

public class PluginOnAuthorizationPublishCompleted {
    private final ChannelHandlerContext ctx;
    private final InternalPublish publish;
    private final ClientData clientData;
    private final AuthorizationBehaviour behaviour;
    private final AuthenticationException exception;

    public PluginOnAuthorizationPublishCompleted(
            ChannelHandlerContext ctx,
            InternalPublish publish,
            ClientData clientData,
            AuthorizationBehaviour behaviour,
            AuthenticationException exception) {
        this.ctx = ctx;
        this.publish = publish;
        this.clientData = clientData;
        this.behaviour = behaviour;
        this.exception = exception;
    }

    public ChannelHandlerContext getCtx() {
        return ctx;
    }

    public InternalPublish getPublish() {
        return publish;
    }

    public ClientData getClientData() {
        return clientData;
    }

    public AuthorizationBehaviour getBehaviour() {
        return behaviour;
    }

    public AuthenticationException getException() {
        return exception;
    }

    public long getTimestamp() {
        return this.publish.getTimestamp();
    }
}
