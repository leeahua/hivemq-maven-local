package cc1;

import cb1.ChannelUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebsocketTextFrameHandler
        extends SimpleChannelInboundHandler<TextWebSocketFrame> {
    private static final Logger LOGGER = LoggerFactory.getLogger(WebsocketTextFrameHandler.class);

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame msg) throws Exception {
        Channel channel = ctx.channel();
        channel.disconnect();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Sending websocket text frames is illegal, only binary frames are allowed for MQTT over websockets. Disconnecting client with IP{}.",
                    ChannelUtils.remoteIP(channel));
        }
    }
}
