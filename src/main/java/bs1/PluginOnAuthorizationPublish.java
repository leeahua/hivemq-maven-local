package bs1;

import bu.InternalPublish;
import com.hivemq.spi.callback.exception.AuthenticationException;
import com.hivemq.spi.security.ClientData;
import io.netty.channel.ChannelHandlerContext;

public class PluginOnAuthorizationPublish {
    private final ChannelHandlerContext ctx;
    private final ClientData clientData;
    private final InternalPublish publish;
    private final AuthenticationException exception;

    public PluginOnAuthorizationPublish(ChannelHandlerContext ctx,
                                        ClientData clientData,
                                        InternalPublish publish) {
        this.ctx = ctx;
        this.clientData = clientData;
        this.publish = publish;
        this.exception = null;
    }

    public ChannelHandlerContext getCtx() {
        return ctx;
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

    public long getTimestamp() {
        return this.publish.getTimestamp();
    }
}
