package bf1;

import bc1.ClientSessionQueueEntry;
import com.hivemq.spi.annotations.NotNull;
import com.hivemq.spi.annotations.Nullable;
import com.hivemq.spi.annotations.ReadOnly;
import com.hivemq.spi.annotations.ThreadSafe;

import java.util.Comparator;
import java.util.SortedSet;

@ThreadSafe
public interface ClientSessionQueue {
    Comparator<ClientSessionQueueEntry> ENTRY_COMPARATOR = new ClientSessionQueueEntityComparator();

    void offer(@NotNull ClientSessionQueueEntry entry);

    @Nullable
    ClientSessionQueueEntry poll();

    void clear();

    void remove(@NotNull String id, long timestamp);

    long size();

    @ReadOnly
    SortedSet<ClientSessionQueueEntry> getAll();

    ClientSessionQueueEntry peek();

    class ClientSessionQueueEntityComparator implements Comparator<ClientSessionQueueEntry> {

        @Override
        public int compare(ClientSessionQueueEntry o1, ClientSessionQueueEntry o2) {
            int result = Long.compare(o1.getTimestamp(), o2.getTimestamp());
            if (result != 0) {
                return result;
            }
            result = o1.getClusterId().compareTo(o2.getClusterId());
            if (result != 0) {
                return result;
            }
            return Long.compare(o1.getSequence(), o2.getSequence());
        }
    }
}
