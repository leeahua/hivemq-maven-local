package bn;

import bv1.PluginOnPing;
import cb1.AttributeKeys;
import cb1.ChannelUtils;
import com.hivemq.spi.callback.lowlevel.OnPingCallback;
import com.hivemq.spi.callback.registry.CallbackRegistry;
import com.hivemq.spi.message.PingReq;
import com.hivemq.spi.message.PingResp;
import cs.ClientToken;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@ChannelHandler.Sharable
public class MqttPingReqHandler extends SimpleChannelInboundHandler<PingReq> {
    private static final Logger LOGGER = LoggerFactory.getLogger(MqttPingReqHandler.class);
    private static final PingResp PING_RESP = new PingResp();
    private final CallbackRegistry callbackRegistry;

    @Inject
    MqttPingReqHandler(CallbackRegistry callbackRegistry) {
        this.callbackRegistry = callbackRegistry;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, PingReq msg) throws Exception {
        if (this.LOGGER.isTraceEnabled()) {
            this.LOGGER.trace("PingReq received for client {}.", ctx.channel().attr(AttributeKeys.MQTT_CLIENT_ID).get());
        }
        ctx.writeAndFlush(PING_RESP);
        if (this.LOGGER.isTraceEnabled()) {
            this.LOGGER.trace("PingResp sent for client {}.", ctx.channel().attr(AttributeKeys.MQTT_CLIENT_ID).get());
        }
        if (this.callbackRegistry.isAvailable(OnPingCallback.class)) {
            ClientToken clientToken = ChannelUtils.clientToken(ctx.channel());
            ctx.fireUserEventTriggered(new PluginOnPing(clientToken));
        }
    }
}
