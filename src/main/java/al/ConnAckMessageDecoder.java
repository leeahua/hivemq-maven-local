package al;

import cb1.AttributeKeys;
import cb1.ChannelUtils;
import com.hivemq.spi.message.ConnAck;
import com.hivemq.spi.message.ProtocolVersion;
import com.hivemq.spi.message.ReturnCode;
import d.CacheScoped;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@CacheScoped
public class ConnAckMessageDecoder extends AbstractMessageDecoder<ConnAck> {
    private static final Logger LOGGER = LoggerFactory.getLogger(PubAckMessageDecoder.class);
    public static final int SESSION_PRESENT_FLAG = 1;

    public ConnAck decode(Channel channel, ByteBuf remainingByteBuf, byte fixedHeader) {
        boolean isV311 = ProtocolVersion.MQTTv3_1_1 == channel.attr(AttributeKeys.MQTT_VERSION).get();
        int variableHeader = remainingByteBuf.readByte();
        if (isV311) {
            if (!validReservedFlagsBits(fixedHeader)) {
                LOGGER.error("A client (IP: {}) sent a Connack with an invalid fixed header. Disconnecting client.",
                        ChannelUtils.remoteIP(channel).orElse("UNKNOWN"));
                channel.close();
                remainingByteBuf.clear();
                return null;
            }
            if (variableHeader != 0 && variableHeader != 1) {
                LOGGER.error("A client (IP: {}) sent a Connack with an invalid variable header. Disconnecting client.",
                        ChannelUtils.remoteIP(channel).orElse("UNKNOWN"));
                channel.close();
                remainingByteBuf.clear();
                return null;
            }
        }
        boolean sessionPresent = false;
        if (isV311) {
            sessionPresent = (variableHeader & 0x1) == SESSION_PRESENT_FLAG;
        }
        int returnCode = remainingByteBuf.readByte();
        return new ConnAck(ReturnCode.with(returnCode), sessionPresent);
    }
}
