package bg1;

import bz.RetainedMessage;
import com.hivemq.spi.annotations.NotNull;
import com.hivemq.spi.message.QoS;

public class RetainedMessageEntity extends RetainedMessage {
    public static final byte[] DELETED_MESSAGE = new byte[0];
    private final boolean deleted;
    private final long timestamp;

    public RetainedMessageEntity(long timestamp) {
        super(DELETED_MESSAGE, QoS.AT_MOST_ONCE);
        this.deleted = true;
        this.timestamp = timestamp;
    }

    public RetainedMessageEntity(@NotNull byte[] message, @NotNull QoS qoS, long timestamp) {
        super(message, qoS);
        this.deleted = false;
        this.timestamp = timestamp;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
