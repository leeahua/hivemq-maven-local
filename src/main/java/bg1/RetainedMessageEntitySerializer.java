package bg1;

import bz.RetainedMessage;
import cb1.ByteUtils;
import com.google.common.base.Preconditions;
import com.google.common.primitives.Longs;
import com.hivemq.spi.annotations.NotNull;
import com.hivemq.spi.message.QoS;

import java.nio.charset.StandardCharsets;

public class RetainedMessageEntitySerializer {
    public static final byte DEFAULT_TIMESTAMP_DELETED_BYTE = Byte.MIN_VALUE;

    @NotNull
    public byte[] serializeTopic(@NotNull String topic) {
        Preconditions.checkNotNull(topic, "Topic must not be null");
        return topic.getBytes(StandardCharsets.UTF_8);
    }


    @NotNull
    public String deserializeTopic(@NotNull byte[] data) {
        Preconditions.checkNotNull(data, "Byte array must not be null");
        return new String(data, 0, data.length, StandardCharsets.UTF_8);
    }

    @NotNull
    public byte[] serializeEntry(@NotNull RetainedMessage retainedMessage, long timestamp) {
        byte[] messageByte = retainedMessage.getMessage();
        byte[] data = new byte[9 + messageByte.length];
        data[0] = ((byte) (0x0 | (byte) retainedMessage.getQoS().getQosNumber()));
        System.arraycopy(Longs.toByteArray(timestamp), 0, data, 1, 8);
        System.arraycopy(messageByte, 0, data, 9, messageByte.length);
        return data;
    }

    @NotNull
    public RetainedMessageEntity deserializeEntry(@NotNull byte[] data) {
        Preconditions.checkNotNull(data, "Byte array must not be null");
        long timestamp = ByteUtils.getLong(data, 1);
        if (ByteUtils.getBoolean(data[0], 7)) {
            return new RetainedMessageEntity(timestamp);
        }
        QoS qoS = QoS.valueOf(data[0] & 0x3);
        byte[] messageBytes = new byte[data.length - 9];
        System.arraycopy(data, 9, messageBytes, 0, data.length - 9);
        return new RetainedMessageEntity(messageBytes, qoS, timestamp);
    }


    public byte[] serializeTimestamp(long timestamp) {
        byte[] data = new byte[9];
        data[0] = DEFAULT_TIMESTAMP_DELETED_BYTE;
        System.arraycopy(Longs.toByteArray(timestamp), 0, data, 1, 8);
        return data;
    }
}
