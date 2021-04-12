package bj;

import cb1.ChannelUtils;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MqttKeepAliveIdleHandler extends ChannelInboundHandlerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(MqttKeepAliveIdleHandler.class);

    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent &&
                ((IdleStateEvent) evt).state() == IdleState.READER_IDLE) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Client with IP {} disconnected. The client was idle for too long without sending a MQTT control packet",
                        ChannelUtils.remoteIP(ctx.channel()).orElse("UNKNOWN"));
            }
            ctx.close();
            return;
        }
        super.userEventTriggered(ctx, evt);
    }
}
