package bg1;

import bi.CachedMessages;
import bu.InternalPublish;
import com.google.common.base.Preconditions;
import com.google.common.primitives.Longs;
import com.hivemq.spi.annotations.NotNull;
import com.hivemq.spi.message.MessageWithId;
import com.hivemq.spi.message.PubRec;
import com.hivemq.spi.message.PubRel;
import com.hivemq.spi.message.QoS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

public class OutgoingMessageFlowSerializer {
    private static final Logger LOGGER = LoggerFactory.getLogger(OutgoingMessageFlowSerializer.class);
    private static final byte[] PUB_REC_BYTES = {0};
    private static final byte[] PUB_REL_BYTES = {-64};
    private final CachedMessages cachedMessages = CachedMessages.INSTANCE;

    @NotNull
    public byte[] serializeKey(@NotNull String clientId, int messageId) {
        Preconditions.checkNotNull(clientId, "Client must not be null");
        Preconditions.checkArgument((messageId >= 0) && (messageId <= 65535), "Message id must be between 0 and 65535. Was %s", messageId);
        byte[] clientIdBytes = clientId.getBytes(StandardCharsets.UTF_8);
        byte[] messageIdBytes = serializeMessageId(messageId);
        byte[] data = new byte[clientIdBytes.length + messageIdBytes.length];
        System.arraycopy(clientIdBytes, 0, data, 0, clientIdBytes.length);
        System.arraycopy(messageIdBytes, 0, data, clientIdBytes.length, messageIdBytes.length);
        return data;
    }

    @NotNull
    public OutgoingMessageFlowKey deserializeKey(@NotNull byte[] data) {
        Preconditions.checkNotNull(data, "Byte array must not be null");
        int messageId = deserializeMessageId(data, data.length - 2);
        String clientId = new String(data, 0, data.length - 2, StandardCharsets.UTF_8);
        return new OutgoingMessageFlowKey(clientId, messageId);
    }

    @NotNull
    public byte[] serializeMessage(@NotNull MessageWithId message) {
        Preconditions.checkArgument(message instanceof InternalPublish || message instanceof PubRec || message instanceof PubRel,
                "Message must be a Publish or a PubRec or a PubRel");
        if (message instanceof PubRec) {
            return PUB_REC_BYTES;
        }
        if (message instanceof PubRel) {
            return PUB_REL_BYTES;
        }
        return serializePublish((InternalPublish) message);
    }

    @NotNull
    public MessageWithId deserializeMessage(@NotNull byte[] data) {
        Preconditions.checkNotNull(data, "Byte array must not be null");
        if (data[0] == PUB_REC_BYTES[0]) {
            return this.cachedMessages.getPubRec(0);
        }
        if (data[0] == PUB_REL_BYTES[0]) {
            return this.cachedMessages.getPubRel(0);
        }
        if ((data[0] & 0x80) == 128) {
            return deserializePublish(data);
        }
        LOGGER.error("Could not deserialize!");
        throw new IllegalArgumentException("Invalid value to deserialize!");
    }

    private byte[] serializePublish(@NotNull InternalPublish publish) {
        int flag = -128;
        flag = (byte) (flag | publish.getQoS().getQosNumber());
        if (publish.isDuplicateDelivery()) {
            flag = (byte) (flag | 0x8);
        }
        if (publish.isRetain()) {
            flag = (byte) (flag | 0x4);
        }
        byte[] topicBytes = publish.getTopic().getBytes(StandardCharsets.UTF_8);
        byte[] timestampBytes = Longs.toByteArray(publish.getTimestamp());
        byte[] sequenceBytes = Longs.toByteArray(publish.getSequence());
        byte[] clusterIdBytes = publish.getClusterId().getBytes(StandardCharsets.UTF_8);
        byte[] payload = publish.getPayload();
        byte[] publishBytes = new byte[1 + topicBytes.length + 2 + 8 + 8 + clusterIdBytes.length + 2 + payload.length];
        int offset = 0;
        publishBytes[offset++] = (byte)flag;
        publishBytes[offset++] = ((byte) (topicBytes.length >> 8 & 0xFF));
        publishBytes[offset++] = ((byte) (topicBytes.length & 0xFF));
        System.arraycopy(topicBytes, 0, publishBytes, offset, topicBytes.length);
        offset += topicBytes.length;
        System.arraycopy(timestampBytes, 0, publishBytes, offset, timestampBytes.length);
        offset += timestampBytes.length;
        System.arraycopy(sequenceBytes, 0, publishBytes, offset, sequenceBytes.length);
        offset += sequenceBytes.length;
        publishBytes[offset++] = ((byte) (clusterIdBytes.length >> 8 & 0xFF));
        publishBytes[offset++] = ((byte) (clusterIdBytes.length & 0xFF));
        System.arraycopy(clusterIdBytes, 0, publishBytes, offset, clusterIdBytes.length);
        offset += clusterIdBytes.length;
        System.arraycopy(payload, 0, publishBytes, offset, payload.length);
        return publishBytes;
    }

    @NotNull
    private InternalPublish deserializePublish(@NotNull byte[] data) {
        int topicLength = data[1] << 8 & 0xFF00 | data[2] & 0xFF;
        String topic = new String(data, 3, topicLength, StandardCharsets.UTF_8);
        int offset = 3 + topicLength;
        long timestamp = deserializeLong(data, offset);
        offset += 8;
        long sequence = deserializeLong(data, offset);
        offset += 8;
        int clusterIdLength = data[offset] << 8 & 0xFF00 | data[(offset + 1)] & 0xFF;
        offset += 2;
        String clusterId = new String(data, offset, clusterIdLength, StandardCharsets.UTF_8);
        offset += clusterIdLength;
        int payloadLength = data.length - offset;
        byte[] payload = new byte[payloadLength];
        System.arraycopy(data, offset, payload, 0, payloadLength);
        InternalPublish publish = new InternalPublish(sequence, timestamp, clusterId);
        QoS qoS = QoS.valueOf(data[0] & 0x3);
        publish.setQoS(qoS);
        publish.setDuplicateDelivery((data[0] & 0x8) == 8);
        publish.setRetain((data[0] & 0x4) == 4);
        publish.setTopic(topic);
        publish.setPayload(payload);
        return publish;
    }


    private long deserializeLong(byte[] data, int offset) {
        return Longs.fromBytes(
                data[offset],
                data[offset + 1],
                data[offset + 2],
                data[offset + 3],
                data[offset + 4],
                data[offset + 5],
                data[offset + 6],
                data[offset + 7]);
    }

    private int deserializeMessageId(@NotNull byte[] data, int offset) {
        return (data[(offset + 1)] & 0xFF) << 8 | data[offset] & 0xFF;
    }

    @NotNull
    private byte[] serializeMessageId(int messageId) {
        return new byte[]{(byte) messageId, (byte) (messageId >> 8)};
    }
}
