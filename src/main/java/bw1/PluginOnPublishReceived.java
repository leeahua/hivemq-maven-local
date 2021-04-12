package bw1;

import bu.InternalPublish;
import com.google.common.base.Preconditions;
import com.hivemq.spi.annotations.NotNull;
import com.hivemq.spi.security.ClientData;
import io.netty.channel.ChannelHandlerContext;

public class PluginOnPublishReceived {
    private final ChannelHandlerContext ctx;
    private final InternalPublish publish;
    private final ClientData clientData;

    public PluginOnPublishReceived(@NotNull InternalPublish publish,
                                   @NotNull ClientData clientData,
                                   @NotNull ChannelHandlerContext ctx) {
        this.ctx = ctx;
        this.publish = Preconditions.checkNotNull(publish, "Publish must not be null");
        this.clientData = Preconditions.checkNotNull(clientData, "ClientData must not be null");
    }

    @NotNull
    public ChannelHandlerContext getCtx() {
        return ctx;
    }

    @NotNull
    public InternalPublish getPublish() {
        return publish;
    }

    @NotNull
    public ClientData getClientData() {
        return clientData;
    }
}
