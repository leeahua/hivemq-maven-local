package cc1;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.ContinuationWebSocketFrame;

public class WebsocketContinuationFrameHandler
        extends SimpleChannelInboundHandler<ContinuationWebSocketFrame> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ContinuationWebSocketFrame msg) throws Exception {
        ctx.fireChannelRead(msg.content().retain());
    }
}
