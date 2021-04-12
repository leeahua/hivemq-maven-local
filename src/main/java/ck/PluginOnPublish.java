package ck;

import bu.InternalPublish;
import com.hivemq.spi.security.ClientData;
import io.netty.channel.ChannelHandlerContext;

public class PluginOnPublish {
    private final InternalPublish publish;
    private final ClientData clientData;
    private final ChannelHandlerContext ctx;

    public PluginOnPublish(ChannelHandlerContext ctx,
                           InternalPublish publish,
                           ClientData clientData) {
        this.ctx = ctx;
        this.publish = publish;
        this.clientData = clientData;
    }

    public InternalPublish getPublish() {
        return publish;
    }

    public ClientData getClientData() {
        return clientData;
    }

    public ChannelHandlerContext getCtx() {
        return ctx;
    }

    public long getTimestamp() {
        return this.publish.getTimestamp();
    }
}
