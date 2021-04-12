package bj;

import bt1.PluginOnConnAckSend;
import cb1.ChannelUtils;
import com.hivemq.spi.callback.lowlevel.OnConnAckSend;
import com.hivemq.spi.callback.registry.CallbackRegistry;
import com.hivemq.spi.message.ConnAck;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;

import javax.inject.Inject;
import javax.inject.Singleton;

@ChannelHandler.Sharable
@Singleton
public class MqttConnAckHandler extends ChannelOutboundHandlerAdapter {
    private final RemoveFutureListener removeFutureListener = new RemoveFutureListener();
    private final CallbackRegistry callbackRegistry;

    @Inject
    MqttConnAckHandler(CallbackRegistry callbackRegistry) {
        this.callbackRegistry = callbackRegistry;
    }

    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof ConnAck &&
                this.callbackRegistry.isAvailable(OnConnAckSend.class)) {
            ctx.fireUserEventTriggered(new PluginOnConnAckSend(ChannelUtils.clientToken(ctx.channel()), (ConnAck) msg));
        }
        super.write(ctx, msg, promise);
        promise.addListener(this.removeFutureListener);
    }

    private static class RemoveFutureListener implements ChannelFutureListener {
        @Override
        public void operationComplete(ChannelFuture future) throws Exception {
            if (future.isSuccess() &&
                    future.channel().pipeline().get(MqttConnAckHandler.class) != null) {
                future.channel().pipeline().remove(MqttConnAckHandler.class);
            }
        }
    }
}
