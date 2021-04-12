package bj;

import cb1.ChannelUtils;
import com.hivemq.spi.message.Connect;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;

@ChannelHandler.Sharable
@Singleton
public class MqttDisallowSecondConnect extends SimpleChannelInboundHandler<Connect> {
    private static final Logger LOGGER = LoggerFactory.getLogger(MqttDisallowSecondConnect.class);

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Connect msg) throws Exception {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("The client with id {} and IP {} sent a second MQTT Connect message. This is not allowed. Disconnecting client",
                    msg.getClientIdentifier(), ChannelUtils.remoteIP(ctx.channel()).orElse("UNKNOWN"));
        }
        ctx.channel().close();
    }
}
