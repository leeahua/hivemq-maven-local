package bk;

import bq1.PluginOnDisconnect;
import cb1.AttributeKeys;
import cb1.ChannelUtils;
import com.hivemq.spi.callback.events.OnDisconnectCallback;
import com.hivemq.spi.callback.registry.CallbackRegistry;
import com.hivemq.spi.message.Disconnect;
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
public class MqttDisconnectHandler extends SimpleChannelInboundHandler<Disconnect> {
    private static final Logger LOGGER = LoggerFactory.getLogger(MqttDisconnectHandler.class);
    private final CallbackRegistry callbackRegistry;
    private boolean receivedDisconnect = false;

    @Inject
    public MqttDisconnectHandler(CallbackRegistry callbackRegistry) {
        this.callbackRegistry = callbackRegistry;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Disconnect msg) throws Exception {
        this.receivedDisconnect = true;
        LOGGER.debug("The client [{}] sent a disconnect message. ", ctx.channel().attr(AttributeKeys.MQTT_CLIENT_ID).get());
        ctx.channel().close();
    }

    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        if (this.callbackRegistry.isAvailable(OnDisconnectCallback.class)) {
            String clientId = ctx.channel().attr(AttributeKeys.MQTT_CLIENT_ID).get();
            if (clientId == null) {
                LOGGER.debug("Client with ip: {}, disconnected without sending a connect message. Disconnect callbacks will be ignored.",
                        ChannelUtils.remoteIP(ctx.channel()).orElse("UNKNOWN"));
            } else {
                ClientToken clientToken = ChannelUtils.clientToken(ctx.channel());
                ctx.fireUserEventTriggered(new PluginOnDisconnect(clientToken, this.receivedDisconnect));
            }
        }
        super.channelInactive(ctx);
    }


}
