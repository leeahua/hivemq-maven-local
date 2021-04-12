package bw1;

import bu.InternalPublish;
import com.google.common.base.Preconditions;
import com.hivemq.spi.annotations.NotNull;
import com.hivemq.spi.callback.events.OnPublishReceivedCallback;
import com.hivemq.spi.security.ClientData;
import io.netty.channel.ChannelHandlerContext;

import java.util.Queue;

public class PluginOnPublishReceivedCallbackCompleted {
    private final ChannelHandlerContext ctx;
    private final Queue<OnPublishReceivedCallback> leftCallbacks;
    private final ClientData clientData;
    private final InternalPublish publish;

    public PluginOnPublishReceivedCallbackCompleted(
            @NotNull ChannelHandlerContext ctx,
            @NotNull Queue<OnPublishReceivedCallback> leftCallbacks,
            @NotNull ClientData clientData,
            @NotNull InternalPublish publish) {
        this.ctx = ctx;
        this.leftCallbacks = Preconditions.checkNotNull(leftCallbacks, "Callbacks must not be null");
        this.clientData = Preconditions.checkNotNull(clientData, "ClientData must not be null");
        this.publish = Preconditions.checkNotNull(publish, "Publish must not be null");
    }

    @NotNull
    public ChannelHandlerContext getCtx() {
        return ctx;
    }

    @NotNull
    public Queue<OnPublishReceivedCallback> getLeftCallbacks() {
        return leftCallbacks;
    }

    @NotNull
    public ClientData getClientData() {
        return clientData;
    }

    @NotNull
    public InternalPublish getPublish() {
        return publish;
    }
}
