package al;

import bi.CachedMessages;
import cb1.AttributeKeys;
import cb1.ChannelUtils;
import com.hivemq.spi.message.ProtocolVersion;
import com.hivemq.spi.message.PubRec;
import d.CacheScoped;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@CacheScoped
public class PubRecMessageDecoder
        extends AbstractMessageDecoder<PubRec> {
    private static final Logger LOGGER = LoggerFactory.getLogger(PubRecMessageDecoder.class);
    private final CachedMessages cachedMessages = CachedMessages.INSTANCE;

    public PubRec decode(Channel channel, ByteBuf remainingByteBuf, byte fixedHeader) {
        if (ProtocolVersion.MQTTv3_1_1 == channel.attr(AttributeKeys.MQTT_VERSION).get() &&
                !validReservedFlagsBits(fixedHeader)) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("A client (IP: {}) sent a Pubrec with an invalid fixed header. Disconnecting client.",
                        ChannelUtils.remoteIP(channel).orElse("UNKNOWN"));
            }
            channel.close();
            remainingByteBuf.clear();
            return null;
        }
        if (remainingByteBuf.readableBytes() < 2) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("A client (IP: {}) sent a Pubrec without a message id. Disconnecting client.",
                        ChannelUtils.remoteIP(channel).orElse("UNKNOWN"));
            }
            channel.close();
            remainingByteBuf.clear();
            return null;
        }
        int messageId = remainingByteBuf.readUnsignedShort();
        return this.cachedMessages.getPubRec(messageId);
    }
}
