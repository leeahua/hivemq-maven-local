package al;

import cb1.AttributeKeys;
import cb1.ByteBufUtils;
import cb1.ChannelUtils;
import com.hivemq.spi.message.ProtocolVersion;
import com.hivemq.spi.message.QoS;
import com.hivemq.spi.message.Subscribe;
import com.hivemq.spi.message.Topic;
import d.CacheScoped;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

@CacheScoped
public class SubscribeMessageDecoder
        extends AbstractMessageDecoder<Subscribe> {
    private static final Logger LOGGER = LoggerFactory.getLogger(SubscribeMessageDecoder.class);


    public Subscribe decode(Channel channel, ByteBuf remainingByteBuf, byte fixedHeader) {
        if (ProtocolVersion.MQTTv3_1_1 == channel.attr(AttributeKeys.MQTT_VERSION).get()) {
            if ((fixedHeader & 0xF) != 2) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.error("A client (IP: {}) sent a subscribe message with an invalid fixed header. Disconnecting client.",
                            ChannelUtils.remoteIP(channel).orElse("UNKNOWN"));
                }
                channel.close();
                remainingByteBuf.clear();
                return null;
            }
        } else if (ProtocolVersion.MQTTv3_1 == channel.attr(AttributeKeys.MQTT_VERSION).get() &&
                (fixedHeader & 0xF) > 3) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("A client (IP: {}) sent a subscribe message with an invalid fixed header. Disconnecting client.",
                        ChannelUtils.remoteIP(channel).orElse("UNKNOWN"));
            }
            channel.close();
            remainingByteBuf.clear();
            return null;
        }
        if (remainingByteBuf.readableBytes() < 2) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("A client (IP: {}) sent a subscribe message with an invalid message id. Disconnecting client.",
                        ChannelUtils.remoteIP(channel).orElse("UNKNOWN"));
            }
            channel.close();
            remainingByteBuf.clear();
            return null;
        }
        int messageId = remainingByteBuf.readUnsignedShort();
        Subscribe subscribe = new Subscribe();
        subscribe.setMessageId(messageId);
        List<Topic> subscriptions = new ArrayList<>();
        while (remainingByteBuf.isReadable()) {
            String topic = ByteBufUtils.decode(remainingByteBuf);
            if (invalidTopic(channel, topic)) {
                channel.close();
                remainingByteBuf.clear();
                return null;
            }
            if (remainingByteBuf.readableBytes() == 0) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("A client (IP: {}) sent a subscribe message which contained no QoS. Disconnecting client.",
                            ChannelUtils.remoteIP(channel).orElse("UNKNOWN"));
                }
                channel.close();
                remainingByteBuf.clear();
                return null;
            }
            int qoSNumber = remainingByteBuf.readByte();
            if (qoSNumber < 0 || qoSNumber > 2) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("A client (IP: {}) sent a subscribe message which contained an invalid QoS subscription. Disconnecting client.",
                            ChannelUtils.remoteIP(channel).orElse("UNKNOWN"));
                }
                channel.close();
                remainingByteBuf.clear();
                return null;
            }
            subscriptions.add(new Topic(topic, QoS.valueOf(qoSNumber)));
        }
        if (subscriptions.isEmpty()) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("A client (IP: {}) sent a subscribe message which didn't contain any subscription. This is not allowed. Disconnecting client.",
                        ChannelUtils.remoteIP(channel).orElse("UNKNOWN"));
            }
            channel.close();
            remainingByteBuf.clear();
            return null;
        }
        subscribe.setTopics(subscriptions);
        return subscribe;
    }

    private boolean invalidTopic(Channel channel, String topic) {
        if (topic == null || topic.isEmpty()) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("A client (IP: {}) sent a subscribe message which contained an empty topic. This is not allowed. Disconnecting client.",
                        ChannelUtils.remoteIP(channel).orElse("UNKNOWN"));
            }
            return true;
        }
        if (topic.contains("\000")) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("A client (IP: {}) sent a subscribe message which contained the Unicode null character (U+0000). This is not allowed. Disconnecting client.",
                        ChannelUtils.remoteIP(channel).orElse("UNKNOWN"));
            }
            return true;
        }
        return false;
    }
}
