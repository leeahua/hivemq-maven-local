package al;

import cb1.AttributeKeys;
import cb1.ByteBufUtils;
import cb1.ChannelUtils;
import com.hivemq.spi.message.ProtocolVersion;
import com.hivemq.spi.message.Unsubscribe;
import d.CacheScoped;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

@CacheScoped
public class UnsubscribeMessageDecoder
        extends AbstractMessageDecoder<Unsubscribe> {
    private static final Logger LOGGER = LoggerFactory.getLogger(UnsubscribeMessageDecoder.class);


    public Unsubscribe decode(Channel channel, ByteBuf remainingByteBuf, byte fixedHeader) {
        if (ProtocolVersion.MQTTv3_1_1 == channel.attr(AttributeKeys.MQTT_VERSION).get()) {
            if ((fixedHeader & 0xF) != 2) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("A client (IP: {}) sent a unsubscribe message with an invalid fixed header. Disconnecting client.",
                            ChannelUtils.remoteIP(channel).orElse("UNKNOWN"));
                }
                channel.close();
                remainingByteBuf.clear();
                return null;
            }
        } else if (ProtocolVersion.MQTTv3_1 == channel.attr(AttributeKeys.MQTT_VERSION).get() &&
                (fixedHeader & 0xF) > 3) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("A client (IP: {}) sent a unsubscribe message with an invalid fixed header. Disconnecting client.",
                        ChannelUtils.remoteIP(channel).orElse("UNKNOWN"));
            }
            channel.close();
            remainingByteBuf.clear();
            return null;
        }
        if (remainingByteBuf.readableBytes() < 2) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("A client (IP: {}) sent a unsubscribe message with an invalid message id. Disconnecting client.",
                        ChannelUtils.remoteIP(channel).orElse("UNKNOWN"));
            }
            channel.close();
            remainingByteBuf.clear();
            return null;
        }
        int messageId = remainingByteBuf.readUnsignedShort();
        List<String> topics = new ArrayList<>();
        while (remainingByteBuf.isReadable()) {
            String topic = ByteBufUtils.decode(remainingByteBuf);
            if (topic == null || topic.isEmpty()) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("A client (IP: {}) sent a Unsubscribe message with an empty topic. Disconnecting client",
                            ChannelUtils.remoteIP(channel).orElse("UNKNOWN"));
                }
                channel.close();
                remainingByteBuf.clear();
                return null;
            }
            topics.add(topic);
        }
        if (topics.isEmpty()) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("A client (IP: {}) sent a Unsubscribe message with an empty payload. Disconnecting client",
                        ChannelUtils.remoteIP(channel).orElse("UNKNOWN"));
            }
            channel.close();
            remainingByteBuf.clear();
            return null;
        }
        Unsubscribe unsubscribe = new Unsubscribe();
        unsubscribe.setMessageId(messageId);
        unsubscribe.setTopics(topics);
        return unsubscribe;
    }
}
