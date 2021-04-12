package bi1;

import bg1.XodusUtils;
import cb1.ByteUtils;
import com.google.common.base.Preconditions;
import com.google.common.primitives.Longs;
import com.hivemq.spi.annotations.NotNull;
import com.hivemq.spi.annotations.ThreadSafe;
import com.hivemq.spi.message.QoS;
import com.hivemq.spi.message.Topic;
import jetbrains.exodus.ByteIterable;

import java.nio.charset.StandardCharsets;

// TODO:
@ThreadSafe
public class ClientSessionSubscriptionSerializer {
    static final int DEFAULT_QOS_NUMBER = -1;

    @ThreadSafe
    public byte[] serialize(@NotNull Topic topic, long timestamp, long paramLong2) {
        Preconditions.checkNotNull(topic, "Topic must not be null");
        int qoSNumber;
        if (topic.getQoS() != null) {
            qoSNumber = topic.getQoS().getQosNumber();
        } else {
            qoSNumber = DEFAULT_QOS_NUMBER;
        }
        byte[] topicBytes = topic.getTopic().getBytes(StandardCharsets.UTF_8);
        byte[] data = new byte[topicBytes.length + 17];
        data[0] = ((byte) qoSNumber);
        System.arraycopy(Longs.toByteArray(timestamp), 0, data, 1, 8);
        System.arraycopy(Longs.toByteArray(paramLong2), 0, data, 9, 8);
        System.arraycopy(topicBytes, 0, data, 17, topicBytes.length);
        return data;
    }

    @ThreadSafe
    public Topic deserialize(@NotNull byte[] data) {
        Preconditions.checkNotNull(data, "Bytes must not be null");
        Preconditions.checkArgument(data.length > 0, "Bytes must be greater than 1");
        int qoSNumber = data[0];
        String topic = new String(data, 17, data.length - 17, StandardCharsets.UTF_8);
        if (qoSNumber == DEFAULT_QOS_NUMBER) {
            return new Topic(topic, null);
        }
        return new Topic(topic, QoS.valueOf(qoSNumber));
    }

    public Long deserializeTimestamp(byte[] data) {
        int i = (data[0] & 0xFF) + ((data[1] & 0xFF) << 8);
        return ByteUtils.getLong(data, i + 2);
    }

    public Long deserializeVector(byte[] data) {
        return ByteUtils.getLong(data, 1);
    }

    public Topic deserialize(ByteIterable byteIterable) {
        Preconditions.checkNotNull(byteIterable, "ByteIterable must not be null");
        return deserialize(XodusUtils.toBytes(byteIterable));
    }

    public byte[] serializeKey(String clientId, long timestamp) {
        byte[] clientIdBytes = clientId.getBytes(StandardCharsets.UTF_8);
        byte[] data = new byte[clientIdBytes.length + 10];
        data[0] = ((byte) (clientIdBytes.length & 0xFF));
        data[1] = ((byte) (clientIdBytes.length >> 8 & 0xFF));
        System.arraycopy(clientIdBytes, 0, data, 2, clientIdBytes.length);
        System.arraycopy(timestamp, 0, data, clientIdBytes.length + 2, 8);
        return data;
    }


    public ClientSessionSubscriptionKey deserializeKey(byte[] data) {
        int clientIdLength = (data[0] & 0xFF) + ((data[1] & 0xFF) << 8);
        String clientId = new String(data, 2, clientIdLength, StandardCharsets.UTF_8);
        Long timestamp = ByteUtils.getLong(data, clientIdLength + 2);
        return new ClientSessionSubscriptionKey(clientId, timestamp);
    }

    public byte[] serializeClientId(String clientId) {
        byte[] clientIdBytes = clientId.getBytes(StandardCharsets.UTF_8);
        byte[] data = new byte[clientIdBytes.length + 2];
        data[0] = ((byte) (clientIdBytes.length & 0xFF));
        data[1] = ((byte) (clientIdBytes.length >> 8 & 0xFF));
        System.arraycopy(clientIdBytes, 0, data, 2, clientIdBytes.length);
        return data;
    }
}
