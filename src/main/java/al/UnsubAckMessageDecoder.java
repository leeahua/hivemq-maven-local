package al;

import cb1.AttributeKeys;
import cb1.ChannelUtils;
import com.hivemq.spi.message.ProtocolVersion;
import com.hivemq.spi.message.UnsubAck;
import d.CacheScoped;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@CacheScoped
public class UnsubAckMessageDecoder
        extends AbstractMessageDecoder<UnsubAck> {
    private static final Logger LOGGER = LoggerFactory.getLogger(UnsubAckMessageDecoder.class);

    public UnsubAck decode(Channel channel, ByteBuf remainingByteBuf, byte fixedHeader) {
        if (ProtocolVersion.MQTTv3_1_1 == channel.attr(AttributeKeys.MQTT_VERSION).get() &&
                !validReservedFlagsBits(fixedHeader)) {
            LOGGER.error("A client (IP: {}) sent a Unsuback with an invalid fixed header. Disconnecting client.",
                    ChannelUtils.remoteIP(channel).orElse("UNKNOWN"));
            channel.close();
            remainingByteBuf.clear();
            return null;
        }
        int messageId = remainingByteBuf.readUnsignedShort();
        return new UnsubAck(messageId);
    }
}
