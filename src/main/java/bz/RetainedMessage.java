package bz;

import com.google.common.base.Preconditions;
import com.hivemq.spi.annotations.NotNull;
import com.hivemq.spi.message.QoS;

import java.util.Arrays;
import java.util.Objects;

public class RetainedMessage {
    private final byte[] message;
    private final QoS qoS;

    public RetainedMessage(@NotNull byte[] message, @NotNull QoS qoS) {
        Preconditions.checkNotNull(message, "Message must not be null");
        Preconditions.checkNotNull(qoS, "QoS not be null");
        this.message = message;
        this.qoS = qoS;
    }

    public byte[] getMessage() {
        return message;
    }

    public QoS getQoS() {
        return qoS;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RetainedMessage that = (RetainedMessage) o;
        return Arrays.equals(message, that.message) &&
                qoS == that.qoS;
    }

    @Override
    public int hashCode() {
        return Objects.hash(message, qoS);
    }
}
