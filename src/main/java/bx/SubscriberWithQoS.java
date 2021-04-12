package bx;

import cb1.ByteUtils;
import com.google.common.base.Preconditions;
import com.hivemq.spi.annotations.NotNull;
import com.hivemq.spi.annotations.Nullable;

import java.util.Objects;

public class SubscriberWithQoS implements Comparable<SubscriberWithQoS> {
    @NotNull
    private final String subscriber;
    private final int qoSNumber;
    private final byte shared;
    @Nullable
    private final String groupId;

    public SubscriberWithQoS(@NotNull String subscriber, int qoSNumber, byte shared) {
        this(subscriber, qoSNumber, shared, null);
    }

    public SubscriberWithQoS(@NotNull String subscriber, int qoSNumber, byte shared, @Nullable String groupId) {
        this.shared = shared;
        this.groupId = groupId;
        Preconditions.checkNotNull(subscriber, "Subscriber must not be null");
        Preconditions.checkArgument(qoSNumber <= 2 && qoSNumber >= 0, "Quality of Service level must be between 0 and 2");
        this.subscriber = subscriber;
        this.qoSNumber = qoSNumber;
    }

    @NotNull
    public String getSubscriber() {
        return subscriber;
    }

    public int getQosNumber() {
        return qoSNumber;
    }

    public byte getShared() {
        return shared;
    }

    public boolean d() {
        return ByteUtils.getBoolean(this.shared, 0);
    }

    public boolean isShared() {
        return ByteUtils.getBoolean(this.shared, SharedTopicUtils.SHARED_TOPIC_BYTE_POSITION);
    }

    @Nullable
    public String getGroupId() {
        return groupId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SubscriberWithQoS that = (SubscriberWithQoS) o;
        return Objects.equals(subscriber, that.subscriber) &&
                qoSNumber == that.qoSNumber;
    }

    @Override
    public int hashCode() {
        return Objects.hash(subscriber, qoSNumber);
    }

    @Override
    public int compareTo(SubscriberWithQoS subscriberWithQoS) {
        int result = this.subscriber.compareTo(subscriberWithQoS.getSubscriber());
        if (result == 0) {
            return Integer.compare(this.qoSNumber, subscriberWithQoS.getQosNumber());
        }
        return result;
    }

    public String toString() {
        return "SubscriberWithQoS{subscriber='" + this.subscriber + '\'' + ", qos=" + this.qoSNumber + '}';
    }

}
