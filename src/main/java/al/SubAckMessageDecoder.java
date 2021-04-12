package al;

import cb1.AttributeKeys;
import cb1.ChannelUtils;
import com.hivemq.spi.message.ProtocolVersion;
import com.hivemq.spi.message.SubAck;
import d.CacheScoped;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

@CacheScoped
public class SubAckMessageDecoder
        extends AbstractMessageDecoder<SubAck> {
    private static final Logger LOGGER = LoggerFactory.getLogger(PubAckMessageDecoder.class);

    public SubAck decode(Channel channel, ByteBuf remainingByteBuf, byte fixedHeader) {
        if (ProtocolVersion.MQTTv3_1_1 == channel.attr(AttributeKeys.MQTT_VERSION).get() &&
                !validReservedFlagsBits(fixedHeader)) {
            LOGGER.error("A client (IP: {}) sent a Suback with an invalid fixed header. Disconnecting client.",
                    ChannelUtils.remoteIP(channel).orElse("UNKNOWN"));
            channel.close();
            remainingByteBuf.clear();
            return null;
        }
        int messageId = remainingByteBuf.readUnsignedShort();
        List<Byte> grantedQos = new ArrayList<>();
        while (remainingByteBuf.isReadable()) {
            byte qoSNumber = remainingByteBuf.readByte();
            if (qoSNumber < 0 || qoSNumber > 2) {
                LOGGER.error("A client (IP: {}) sent a suback which contained an invalid QoS. Disconnecting client.",
                        ChannelUtils.remoteIP(channel).orElse("UNKNOWN"));
                channel.close();
                remainingByteBuf.clear();
                return null;
            }
            grantedQos.add(qoSNumber);
        }
        return new SubAck(messageId, grantedQos);
    }
}
