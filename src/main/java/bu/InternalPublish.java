package bu;

import com.hivemq.spi.message.Publish;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

public class InternalPublish extends Publish {
    private static final AtomicLong SEQUENCE_PRODUCER = new AtomicLong(1L);
    private final long sequence;
    private final String clusterId;
    private final long timestamp;
    private String uniqueId;

    public InternalPublish(String clusterId) {
        this.clusterId = clusterId;
        this.sequence = SEQUENCE_PRODUCER.getAndIncrement();
        this.timestamp = System.currentTimeMillis();
    }

    public InternalPublish(String clusterId, long timestamp) {
        this(SEQUENCE_PRODUCER.getAndIncrement(), timestamp, clusterId);
    }

    public InternalPublish(long sequence, long timestamp, String clusterId) {
        this.sequence = sequence;
        this.timestamp = timestamp;
        this.clusterId = clusterId;
    }

    public InternalPublish(String clusterId, Publish publish) {
        this(clusterId, publish, System.currentTimeMillis());
    }

    public InternalPublish(String clusterId, Publish publish, long timestamp) {
        this.clusterId = clusterId;
        this.sequence = SEQUENCE_PRODUCER.getAndIncrement();
        this.timestamp = timestamp;
        setPayload(publish.getPayload());
        setTopic(publish.getTopic());
        setQoS(publish.getQoS());
        setMessageId(publish.getMessageId());
        setDuplicateDelivery(publish.isDuplicateDelivery());
        setRetain(publish.isRetain());
    }

    public InternalPublish(String paramString, long paramLong, Publish paramPublish) {
        this.clusterId = paramString;
        this.sequence = paramLong;
        this.timestamp = System.currentTimeMillis();
        setPayload(paramPublish.getPayload());
        setTopic(paramPublish.getTopic());
        setQoS(paramPublish.getQoS());
        setMessageId(paramPublish.getMessageId());
        setDuplicateDelivery(paramPublish.isDuplicateDelivery());
        setRetain(paramPublish.isRetain());
    }

    public InternalPublish(InternalPublish publish) {
        this.clusterId = publish.getClusterId();
        this.sequence = publish.getSequence();
        this.timestamp = publish.getTimestamp();
        setPayload(publish.getPayload());
        setTopic(publish.getTopic());
        setQoS(publish.getQoS());
        setMessageId(publish.getMessageId());
        setDuplicateDelivery(publish.isDuplicateDelivery());
        setRetain(publish.isRetain());
    }

    public static InternalPublish of(Publish publish, String clusterId) {
        Publish copyPublish = Publish.copy(publish);
        if (publish instanceof InternalPublish) {
            InternalPublish internalPublish = (InternalPublish) publish;
            return new InternalPublish(internalPublish.clusterId, internalPublish.sequence, copyPublish);
        }
        return new InternalPublish(clusterId, copyPublish);
    }

    public static InternalPublish of(InternalPublish publish) {
        InternalPublish newPublish = new InternalPublish(publish.getSequence(), publish.getTimestamp(), publish.getClusterId());
        newPublish.setPayload(publish.getPayload());
        newPublish.setTopic(publish.getTopic());
        newPublish.setQoS(publish.getQoS());
        newPublish.setMessageId(publish.getMessageId());
        newPublish.setDuplicateDelivery(publish.isDuplicateDelivery());
        newPublish.setRetain(publish.isRetain());
        return newPublish;
    }

    public String getClusterId() {
        return clusterId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public long getSequence() {
        return sequence;
    }

    public String getUniqueId() {
        if (this.uniqueId == null) {
            this.uniqueId = (this.clusterId + "_pub_" + this.sequence);
        }
        return this.uniqueId;
    }

    public String toString() {
        return "Publish{uniqueId=" + this.clusterId + "_pub_" + this.sequence + ", timestamp=" + this.timestamp + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        InternalPublish that = (InternalPublish) o;
        return sequence == that.sequence &&
                timestamp == that.timestamp &&
                Objects.equals(clusterId, that.clusterId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), sequence, clusterId, timestamp);
    }
}
