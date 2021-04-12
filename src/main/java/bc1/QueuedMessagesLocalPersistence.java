package bc1;

import bu.InternalPublish;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.hivemq.spi.annotations.NotNull;
import u.TimestampObject;
import u.Filter;

import java.util.Set;

public interface QueuedMessagesLocalPersistence {
    void offer(@NotNull String clientId, @NotNull InternalPublish publish, long timestamp);

    boolean queuePublishIfQueueNotEmpty(@NotNull String clientId, @NotNull InternalPublish publish);

    void remove(@NotNull String clientId, long timestamp);

    long size(@NotNull String clientId);

    ClientSessionQueueEntry poll(String clientId, long timestamp);

    void remove(@NotNull String clientId, @NotNull String entryId, long entryTimestamp, long timestamp);

    void clear();

    ClientSessionQueueEntry peek(String clientId);

    ImmutableMap<String, TimestampObject<Set<ClientSessionQueueEntry>>> getEntries(Filter filter);

    void offerAll(ImmutableSet<ClientSessionQueueEntry> queueEntries, String clientId, long timestamp);

    long getTimestamp(String clientId);

    ImmutableSet<String> remove(Filter filter);

    ImmutableSet<String> cleanUp(long tombstoneMaxAge);

    void close();
}
