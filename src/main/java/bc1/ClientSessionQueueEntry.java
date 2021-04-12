package bc1;

import com.google.common.base.Preconditions;
import com.hivemq.spi.annotations.NotNull;
import com.hivemq.spi.message.QoS;

import java.util.Arrays;
import java.util.Objects;

public class ClientSessionQueueEntry {
    private final long sequence;
    private final long timestamp;
    private final String topic;
    private final QoS qoS;
    private final byte[] payload;
    private final String clusterId;

    public ClientSessionQueueEntry(long sequence,
                                   long timestamp,
                                   @NotNull String topic,
                                   @NotNull QoS qoS,
                                   @NotNull byte[] payload,
                                   @NotNull String clusterId) {
        this.clusterId = clusterId;
        Preconditions.checkArgument(timestamp > 0L, "Timestamp must be an actual timestamp");
        Preconditions.checkNotNull(topic, "Topic must not be null");
        Preconditions.checkNotNull(qoS, "QoS must not be null");
        Preconditions.checkNotNull(payload, "Payload must not be null");
        Preconditions.checkNotNull(clusterId, "ClusterId must not be null");
        this.sequence = sequence;
        this.timestamp = timestamp;
        this.topic = topic;
        this.qoS = qoS;
        this.payload = payload;
    }

    @NotNull
    public String getUniqueId() {
        return this.clusterId + "_pub_" + this.sequence;
    }

    public long getTimestamp() {
        return this.timestamp;
    }

    @NotNull
    public String getTopic() {
        return this.topic;
    }

    @NotNull
    public QoS getQoS() {
        return this.qoS;
    }

    @NotNull
    public byte[] getPayload() {
        return this.payload;
    }

    @NotNull
    public String getClusterId() {
        return this.clusterId;
    }

    public long getSequence() {
        return this.sequence;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ClientSessionQueueEntry that = (ClientSessionQueueEntry) o;
        return sequence == that.sequence &&
                timestamp == that.timestamp &&
                Objects.equals(topic, that.topic) &&
                qoS == that.qoS &&
                Arrays.equals(payload, that.payload) &&
                Objects.equals(clusterId, that.clusterId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sequence, timestamp, topic, qoS, payload, clusterId);
    }
}
