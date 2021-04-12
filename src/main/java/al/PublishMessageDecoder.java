package al;

import bu.InternalPublish;
import cb1.ByteBufUtils;
import cb1.ByteUtils;
import cb1.ChannelUtils;
import com.google.inject.Inject;
import com.hivemq.spi.message.Publish;
import com.hivemq.spi.message.QoS;
import d.CacheScoped;
import i.ClusterIdProducer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@CacheScoped
public class PublishMessageDecoder extends AbstractMessageDecoder<Publish> {
    private static final Logger LOGGER = LoggerFactory.getLogger(PublishMessageDecoder.class);
    private final ClusterIdProducer clusterIdProducer;

    @Inject
    public PublishMessageDecoder(ClusterIdProducer clusterIdProducer) {
        this.clusterIdProducer = clusterIdProducer;
    }

    public Publish decode(Channel channel, ByteBuf remainingByteBuf, byte fixedHeader) {
        int qoSNumber = (fixedHeader & 0x6) >> 1;
        if (qoSNumber == 3) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("A client (IP: {}) sent a Publish with an invalid qos. Disconnecting client.",
                        ChannelUtils.remoteIP(channel).orElse("UNKNOWN"));
            }
            channel.close();
            remainingByteBuf.clear();
            return null;
        }
        boolean dup = ByteUtils.getBoolean(fixedHeader, 3);
        if (qoSNumber == 0 && dup) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("A client (IP: {}) sent a Publish with QoS 0 and DUP set to 1. Disconnecting client.",
                        ChannelUtils.remoteIP(channel).orElse("UNKNOWN"));
            }
            channel.close();
            remainingByteBuf.clear();
            return null;
        }
        boolean retain = ByteUtils.getBoolean(fixedHeader, 0);
        String topic = ByteBufUtils.decode(remainingByteBuf);
        if (topic == null) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("A client (IP: {}) sent a Publish with an incorrect topic length. Disconnecting client.",
                        ChannelUtils.remoteIP(channel).orElse("UNKNOWN"));
            }
            channel.close();
            remainingByteBuf.clear();
            return null;
        }
        if (hasWildcard(topic)) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("A client (IP: {}) sent a Publish with a wildcard character (# or +). This is forbidden by the MQTT specification. Disconnecting client.",
                        ChannelUtils.remoteIP(channel).orElse("UNKNOWN"));
            }
            channel.close();
            remainingByteBuf.clear();
            return null;
        }
        int messageId = 0;
        if (qoSNumber > 0) {
            messageId = remainingByteBuf.readUnsignedShort();
            if (messageId == 0) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("A client (IP: {}) sent a Publish with QoS > 0 and message ID 0. Disconnecting client.",
                            ChannelUtils.remoteIP(channel).orElse("UNKNOWN"));
                }
                channel.close();
                remainingByteBuf.clear();
                return null;
            }
        }
        byte[] payload = new byte[remainingByteBuf.readableBytes()];
        remainingByteBuf.readBytes(payload);
        InternalPublish publish = new InternalPublish(this.clusterIdProducer.get());
        publish.setQoS(QoS.valueOf(qoSNumber));
        publish.setTopic(topic);
        publish.setDuplicateDelivery(dup);
        publish.setMessageId(messageId);
        publish.setRetain(retain);
        publish.setPayload(payload);
        return publish;
    }

    public boolean hasWildcard(String topic) {
        return topic.contains("#") || topic.contains("+");
    }
}
