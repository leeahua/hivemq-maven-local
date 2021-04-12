package bz;

import com.google.common.base.Preconditions;
import com.hivemq.spi.annotations.NotNull;
import com.hivemq.spi.message.QoS;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Objects;

public class LWT implements Serializable {
    private final QoS willQoS;
    private final boolean retain;
    private final String willTopic;
    private final byte[] willPayload;

    public LWT(@NotNull String willTopic, @NotNull byte[] willPayload, @NotNull QoS willQoS, boolean retain) {
        Preconditions.checkNotNull(willTopic, "Will Topic must not be null");
        Preconditions.checkNotNull(willPayload, "Will Payload must not be null");
        Preconditions.checkNotNull(willQoS, "Will QoS must not be null");
        this.willTopic = willTopic;
        this.willPayload = willPayload;
        this.willQoS = willQoS;
        this.retain = retain;
    }

    public QoS getWillQoS() {
        return willQoS;
    }

    public boolean isRetain() {
        return retain;
    }

    public String getWillTopic() {
        return willTopic;
    }

    public byte[] getWillPayload() {
        return willPayload;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        LWT lwt = (LWT) o;
        return retain == lwt.retain &&
                willQoS == lwt.willQoS &&
                Objects.equals(willTopic, lwt.willTopic) &&
                Arrays.equals(willPayload, lwt.willPayload);
    }

    @Override
    public int hashCode() {
        return Objects.hash(willQoS, retain, willTopic, willPayload);
    }

    public String toString() {
        return "Topic: " + this.willTopic +
                " QoS: " + this.willQoS.getQosNumber() +
                " retain: " + this.retain +
                " size: " + this.willPayload.length;
    }
}
