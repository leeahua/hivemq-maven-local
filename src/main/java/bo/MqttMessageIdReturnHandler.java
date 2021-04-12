package bo;

import bu.MessageIDPools;
import bv.MessageIdProducer;
import cb1.AttributeKeys;
import com.hivemq.spi.message.MessageWithId;
import com.hivemq.spi.message.PubAck;
import com.hivemq.spi.message.PubComp;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@ChannelHandler.Sharable
public class MqttMessageIdReturnHandler extends ChannelInboundHandlerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(MqttMessageIdReturnHandler.class);
    private final MessageIDPools messageIDPools;

    @Inject
    MqttMessageIdReturnHandler(MessageIDPools messageIDPools) {
        this.messageIDPools = messageIDPools;
    }

    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        super.channelRead(ctx, msg);
        if (!(msg instanceof PubAck) && !(msg instanceof PubComp)) {
            return;
        }
        int messageId = ((MessageWithId) msg).getMessageId();
        if (messageId <= 0) {
            return;
        }
        String clientId = ctx.channel().attr(AttributeKeys.MQTT_CLIENT_ID).get();
        if (clientId == null) {
            LOGGER.warn("Could not return message id {} to the pool because there was an empty client id", messageId);
            return;
        }
        MessageIdProducer producer = this.messageIDPools.forClient(clientId);
        producer.remove(messageId);
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Returning Message ID {} for client {} because of a {} message was received",
                    messageId, clientId, msg.getClass().getSimpleName());
        }
    }
}
