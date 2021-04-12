package v1;

import ak.VectorClock;
import bc1.ClientSessionQueueEntry;
import com.google.common.collect.ImmutableSet;

public class MessageQueueReplication {
    private final VectorClock vectorClock;
    private final long timestamp;
    private final ImmutableSet<ClientSessionQueueEntry> entries;

    public MessageQueueReplication(VectorClock vectorClock,
                                   long timestamp,
                                   ImmutableSet<ClientSessionQueueEntry> entries) {
        this.vectorClock = vectorClock;
        this.timestamp = timestamp;
        this.entries = entries;
    }

    public VectorClock getVectorClock() {
        return vectorClock;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public ImmutableSet<ClientSessionQueueEntry> getEntries() {
        return entries;
    }
}
