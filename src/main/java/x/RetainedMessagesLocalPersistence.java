package x;

import bc1.CloseablePersistence;
import bz.RetainedMessage;
import com.hivemq.spi.annotations.NotNull;
import com.hivemq.spi.annotations.Nullable;
import u.TimestampObject;
import u.Filter;

import java.util.Map;
import java.util.Set;

public interface RetainedMessagesLocalPersistence extends CloseablePersistence {
    long size();

    void clear(int bucketIndex);

    void remove(@NotNull String topic, long timestamp);

    @Nullable
    RetainedMessage get(@NotNull String topic);

    @Nullable
    Long getTimestamp(@NotNull String topic);

    void addOrReplace(@NotNull String topic, @NotNull RetainedMessage retainedMessage, long timestamp);

    Map<String, TimestampObject<RetainedMessage>> getEntries(@NotNull Filter filter, int bucketIndex);

    Set<String> getTopics(@NotNull Filter filter, int bucketIndex);

    Set<String> removeAll(@NotNull Filter filter, int bucketIndex);

    Set<String> cleanUp(long tombstoneMaxAge, int bucketIndex);
}
