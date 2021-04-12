package bf1;

import bc1.ClientSessionQueueEntry;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSortedSet;
import com.hivemq.spi.annotations.NotNull;
import com.hivemq.spi.annotations.Nullable;
import com.hivemq.spi.annotations.ThreadSafe;

import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.SortedSet;

@ThreadSafe
public class BufferedDiscardOldestClientSessionQueue implements ClientSessionQueue {
    public static int BUFFER = 100;
    private static final int INITIAL_CAPACITY = 11;
    private final long maxQueuedMessages;
    private final PriorityQueue<ClientSessionQueueEntry> entryQueue;

    protected BufferedDiscardOldestClientSessionQueue(long maxQueuedMessages) {
        this.maxQueuedMessages = maxQueuedMessages;
        this.entryQueue = new PriorityQueue(INITIAL_CAPACITY, ENTRY_COMPARATOR);
    }

    public synchronized void offer(@NotNull ClientSessionQueueEntry entry) {
        Preconditions.checkNotNull(entry, "ClientSessionQueueEntry must not be null");
        this.entryQueue.offer(entry);
        if (this.entryQueue.size() > this.maxQueuedMessages + BUFFER) {
            ClientSessionQueueEntry pollEntry = this.entryQueue.poll();
        }
    }

    @Nullable
    public ClientSessionQueueEntry poll() {
        pollOverflowEntries();
        return this.entryQueue.poll();
    }

    private void pollOverflowEntries() {
        if (this.entryQueue.size() > this.maxQueuedMessages) {
            for (int index = this.entryQueue.size(); index > this.maxQueuedMessages; index--) {
                this.entryQueue.poll();
            }
        }
    }

    public synchronized long size() {
        if (this.entryQueue.size() > this.maxQueuedMessages) {
            return this.maxQueuedMessages;
        }
        return this.entryQueue.size();
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

    public synchronized SortedSet<ClientSessionQueueEntry> getAll() {
        pollOverflowEntries();
        return ImmutableSortedSet.copyOf(ENTRY_COMPARATOR, this.entryQueue);
    }

    public ClientSessionQueueEntry peek() {
        return this.entryQueue.peek();
    }
}
