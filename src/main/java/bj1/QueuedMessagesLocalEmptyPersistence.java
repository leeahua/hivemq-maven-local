package bj1;

import bc1.QueuedMessagesLocalPersistence;
import bc1.ClientSessionQueueEntry;
import bu.InternalPublish;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.hivemq.spi.annotations.NotNull;
import com.hivemq.spi.annotations.Nullable;
import d.CacheScoped;
import u.TimestampObject;
import u.Filter;

import java.util.Set;

@CacheScoped
class QueuedMessagesLocalEmptyPersistence
        implements QueuedMessagesLocalPersistence {
    public void clear() {
    }

    public void close() {
    }

    public void offer(String clientId, InternalPublish publish, long timestamp) {
    }

    @Nullable
    public ClientSessionQueueEntry poll(String clientId, long timestamp) {
        return null;
    }

    public void remove(String clientId, long timestamp) {
    }

    public void remove(@NotNull String clientId, @NotNull String entryId, long entryTimestamp, long timestamp) {
    }

    public ClientSessionQueueEntry peek(String clientId) {
        return null;
    }

    public long size(String clientId) {
        return 0L;
    }

    public boolean queuePublishIfQueueNotEmpty(@NotNull String clientId, @NotNull InternalPublish publish) {
        return false;
    }

    public ImmutableMap<String, TimestampObject<Set<ClientSessionQueueEntry>>> getEntries(Filter filter) {
        return null;
    }

    public void offerAll(ImmutableSet<ClientSessionQueueEntry> queueEntries, String clientId, long timestamp) {
    }

    public long getTimestamp(String clientId) {
        return 0L;
    }

    public ImmutableSet<String> remove(Filter filter) {
        return null;
    }

    public ImmutableSet<String> cleanUp(long tombstoneMaxAge) {
        return null;
    }
}
