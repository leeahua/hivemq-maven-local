package f;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.group.ChannelGroup;

public class AllChannelGroupHandler extends ChannelInboundHandlerAdapter {
    private final ChannelGroup channelGroup;

    public AllChannelGroupHandler(ChannelGroup channelGroup) {
        this.channelGroup = channelGroup;
    }

    public void channelActive(ChannelHandlerContext ctx) {
        this.channelGroup.add(ctx.channel());
        ctx.fireChannelActive();
    }
}
