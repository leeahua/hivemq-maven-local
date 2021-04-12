package bf1;

import bc1.ClientSessionQueueEntry;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.MinMaxPriorityQueue;
import com.google.common.primitives.Ints;
import com.hivemq.spi.annotations.NotNull;
import com.hivemq.spi.annotations.Nullable;
import com.hivemq.spi.annotations.ThreadSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.SortedSet;

@ThreadSafe
public class DiscardClientSessionQueue implements ClientSessionQueue {
    private static final Logger LOGGER = LoggerFactory.getLogger(DiscardClientSessionQueue.class);
    private final MinMaxPriorityQueue<ClientSessionQueueEntry> entryQueue;

    protected DiscardClientSessionQueue(long maxQueuedMessages) {
        Preconditions.checkArgument(maxQueuedMessages > 0L, "Max Entries must be greater than 0");
        if (maxQueuedMessages > Integer.MAX_VALUE) {
            LOGGER.trace("Maximum Entries ({}) is higher than INTEGER.MAX_VALUE ({}). Ignoring that value and using {}",
                    maxQueuedMessages, Integer.MAX_VALUE, Integer.MAX_VALUE);
        }
        this.entryQueue = MinMaxPriorityQueue.orderedBy(ENTRY_COMPARATOR)
                .maximumSize(Ints.saturatedCast(maxQueuedMessages))
                .create();
    }

    public synchronized void offer(@NotNull ClientSessionQueueEntry entry) {
        this.entryQueue.add(entry);
    }

    @Nullable
    public synchronized ClientSessionQueueEntry poll() {
        return this.entryQueue.poll();
    }

    public synchronized void clear() {
        this.entryQueue.clear();
    }

    public synchronized void remove(@NotNull String id, long timestamp) {
        Preconditions.checkNotNull(id, "id must not be null");
        Preconditions.checkArgument(timestamp > 0L, "Timestamp must be greater than 0");
        Iterator<ClientSessionQueueEntry> iterator = this.entryQueue.iterator();
        while (iterator.hasNext()) {
            ClientSessionQueueEntry entry = iterator.next();
            if (entry.getUniqueId().equals(id) && entry.getTimestamp() == timestamp) {
                iterator.remove();
                break;
            }
        }
    }

    public synchronized long size() {
        return this.entryQueue.size();
    }

    public synchronized SortedSet<ClientSessionQueueEntry> getAll() {
        return ImmutableSortedSet.copyOf(ENTRY_COMPARATOR, this.entryQueue);
    }

    public ClientSessionQueueEntry peek() {
        return this.entryQueue.peek();
    }
}
