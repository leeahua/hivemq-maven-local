package u;

import com.google.common.base.Preconditions;
import com.hivemq.spi.annotations.Nullable;

public class TimestampObject<T> {
    private long timestamp;
    private final T object;

    public TimestampObject(@Nullable T object, long timestamp) {
        Preconditions.checkNotNull(object, "object must not be null");
        this.timestamp = timestamp;
        this.object = object;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public T getObject() {
        return object;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String toString() {
        return this.object.toString();
    }
}
