package bc1;

import com.google.common.collect.ImmutableSet;
import com.hivemq.spi.annotations.NotNull;
import com.hivemq.spi.annotations.ReadOnly;
import com.hivemq.spi.message.Topic;
import u.TimestampObject;
import u.Filter;

import java.util.Map;
import java.util.Set;

public interface ClientSessionSubscriptionsLocalPersistence {
    void addSubscription(@NotNull String clientId, @NotNull Topic topic, long timestamp);

    @ReadOnly
    ImmutableSet<Topic> getSubscriptions(@NotNull String clientId);

    void removeAllSubscriptions(@NotNull String clientId, long timestamp);

    void removeSubscription(@NotNull String clientId, @NotNull Topic topic, long timestamp);

    Long getTimestamp(@NotNull String clientId);

    Map<String, TimestampObject<Set<Topic>>> getEntries(@NotNull Filter filter);

    Set<String> remove(Filter filter);

    Set<String> cleanUp(long tombstoneMaxAge);

    void close();
}
