package aq1;

import am1.Metrics;
import com.hivemq.spi.message.Connect;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@ChannelHandler.Sharable
public class MqttConnectionCounterHandler extends ChannelInboundHandlerAdapter {
    private final MqttConnectionCounter counter;
    private final Metrics metrics;

    @Inject
    public MqttConnectionCounterHandler(MqttConnectionCounter counter, Metrics metrics) {
        this.counter = counter;
        this.metrics = metrics;
    }

    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof Connect) {
            this.counter.increase();
            this.metrics.connectionsOverallMean().update(this.counter.currentConnections());
            ctx.channel().closeFuture().addListener(future -> this.counter.decrease());
        }
        super.channelRead(ctx, msg);
    }
}
