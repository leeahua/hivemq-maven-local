package al;

import cb1.AttributeKeys;
import cb1.ChannelUtils;
import com.hivemq.spi.message.Disconnect;
import com.hivemq.spi.message.ProtocolVersion;
import d.CacheScoped;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@CacheScoped
public class DisconnectMessageDecoder extends AbstractMessageDecoder<Disconnect> {
    private static final Logger LOGGER = LoggerFactory.getLogger(DisconnectMessageDecoder.class);
    private static final Disconnect DISCONNECT = new Disconnect();

    public Disconnect decode(Channel channel, ByteBuf remainingByteBuf, byte fixedHeader) {
        if (ProtocolVersion.MQTTv3_1_1 == channel.attr(AttributeKeys.MQTT_VERSION).get() &&
                !validReservedFlagsBits(fixedHeader)) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("A client (IP: {}) sent a disconnect message with an invalid fixed header. Disconnecting client.",
                        ChannelUtils.remoteIP(channel).orElse("UNKNOWN"));
            }
            channel.close();
            remainingByteBuf.clear();
            return null;
        }
        return DISCONNECT;
    }
}
