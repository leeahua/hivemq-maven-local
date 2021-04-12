package bj1;

import bc1.ClientSessionQueueEntry;
import com.google.common.primitives.Bytes;
import com.google.common.primitives.Longs;
import com.hivemq.spi.annotations.NotNull;
import com.hivemq.spi.annotations.ThreadSafe;
import com.hivemq.spi.message.QoS;

import java.nio.charset.StandardCharsets;

@ThreadSafe
public class QueuedMessagesSerializer {
    public static final int TIMESTAMP_BYTE_LENGTH = 6;

    @NotNull
    public byte[] serialize(@NotNull ClientSessionQueueEntry entry) {
        byte[] timestampBytes = serializeTimestamp(entry.getTimestamp());
        byte[] clusterIdBytes = entry.getClusterId().getBytes(StandardCharsets.UTF_8);
        byte[] topicBytes = entry.getTopic().getBytes(StandardCharsets.UTF_8);
        byte qoSNumberBytes = (byte) entry.getQoS().getQosNumber();
        byte[] payload = entry.getPayload();
        byte[] data = new byte[TIMESTAMP_BYTE_LENGTH + clusterIdBytes.length + 2 + topicBytes.length + 2 + 8 + 1 + payload.length];
        System.arraycopy(timestampBytes, 0, data, 0, TIMESTAMP_BYTE_LENGTH);
        data[6] = ((byte) (clusterIdBytes.length >> 8 & 0xFF));
        data[7] = ((byte) (clusterIdBytes.length & 0xFF));
        System.arraycopy(clusterIdBytes, 0, data, 8, clusterIdBytes.length);
        System.arraycopy(Longs.toByteArray(entry.getSequence()), 0, data, 8 + clusterIdBytes.length, 8);
        data[(16 + clusterIdBytes.length)] = ((byte) (topicBytes.length >> 8 & 0xFF));
        data[(17 + clusterIdBytes.length)] = ((byte) (topicBytes.length & 0xFF));
        System.arraycopy(topicBytes, 0, data, 18 + clusterIdBytes.length, topicBytes.length);
        data[(18 + clusterIdBytes.length + topicBytes.length)] = qoSNumberBytes;
        System.arraycopy(payload, 0, data, 19 + clusterIdBytes.length + topicBytes.length, payload.length);
        return data;
    }

    @NotNull
    public ClientSessionQueueEntry deserialize(@NotNull byte[] data) {
        long timestamp = deserializeTimestamp(data);
        int clusterIdLength = (data[6] & 0xFF) << 8 | data[7] & 0xFF;
        String clusterId = new String(data, 8, clusterIdLength, StandardCharsets.UTF_8);
        long sequence = deserializeSequence(data, 8 + clusterIdLength);
        int topicLength = (data[(16 + clusterIdLength)] & 0xFF) << 8 | data[(17 + clusterIdLength)] & 0xFF;
        String topic = new String(data, 18 + clusterIdLength, topicLength, StandardCharsets.UTF_8);
        QoS qoS = QoS.valueOf(data[(18 + clusterIdLength + topicLength)]);
        int payloadLength = data.length - 19 - clusterIdLength - topicLength;
        byte[] payload = new byte[payloadLength];
        System.arraycopy(data, 19 + clusterIdLength + topicLength, payload, 0, payloadLength);
        return new ClientSessionQueueEntry(sequence, timestamp, topic,
                qoS, payload, clusterId);
    }

    @NotNull
    public byte[] serialize(long timestamp, @NotNull String clusterId, long sequence) {
        byte[] clusterIdBytes = clusterId.getBytes(StandardCharsets.UTF_8);
        byte[] clusterIdLength = new byte[2];
        clusterIdLength[0] = ((byte) (clusterIdBytes.length >> 8 & 0xFF));
        clusterIdLength[1] = ((byte) (clusterIdBytes.length & 0xFF));
        return Bytes.concat(new byte[][]{serializeTimestamp(timestamp),
                clusterIdLength,
                clusterIdBytes,
                Longs.toByteArray(sequence)});
    }

    public long deserializeTimestamp(@NotNull byte[] data) {
        return (data[5] & 0xFF) << 40 |
                (data[4] & 0xFF) << 32 |
                (data[3] & 0xFF) << 24 |
                (data[2] & 0xFF) << 16 |
                (data[1] & 0xFF) << 8 |
                data[0] & 0xFF;
    }

    @NotNull
    public byte[] serializeTimestamp(long timestamp) {
        return new byte[]{
                (byte) (int) timestamp,
                (byte) (int) (timestamp >> 8),
                (byte) (int) (timestamp >> 16),
                (byte) (int) (timestamp >> 24),
                (byte) (int) (timestamp >> 32),
                (byte) (int) (timestamp >> 40)
        };
    }

    private long deserializeSequence(byte[] data, int offset) {
        return Longs.fromBytes(
                data[offset],
                data[(offset + 1)],
                data[(offset + 2)],
                data[(offset + 3)],
                data[(offset + 4)],
                data[(offset + 5)],
                data[(offset + 6)],
                data[(offset + 7)]);
    }
}
