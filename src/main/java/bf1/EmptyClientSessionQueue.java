package bf1;

import bc1.ClientSessionQueueEntry;
import com.google.common.collect.ImmutableSortedSet;
import com.hivemq.spi.annotations.NotNull;
import com.hivemq.spi.annotations.Nullable;

import java.util.SortedSet;

public class EmptyClientSessionQueue implements ClientSessionQueue {
    public static EmptyClientSessionQueue INSTANCE = new EmptyClientSessionQueue();

    public void offer(@NotNull ClientSessionQueueEntry entry) {
    }

    @Nullable
    public ClientSessionQueueEntry poll() {
        return null;
    }

    public void clear() {
    }

    public void remove(@NotNull String id, long timestamp) {
    }

    public long size() {
        return 0L;
    }

    public SortedSet<ClientSessionQueueEntry> getAll() {
        return ImmutableSortedSet.of();
    }

    public ClientSessionQueueEntry peek() {
        return null;
    }
}
