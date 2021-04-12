package be1;

import bc1.ClientSessionSubscriptionsLocalPersistence;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.hivemq.spi.annotations.NotNull;
import com.hivemq.spi.annotations.Nullable;
import com.hivemq.spi.message.Topic;
import d.CacheScoped;
import u.Filter;
import u.TimestampObject;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@CacheScoped
public class ClientSessionSubscriptionsLocalMemoryPersistence
        implements ClientSessionSubscriptionsLocalPersistence {
    private final Map<String, TimestampObject<Set<Topic>>> store = new ConcurrentHashMap<>();

    public Set<String> remove(@NotNull Filter filter) {
        Preconditions.checkNotNull(filter, "Filter must not be null");
        Set<String> removedClients = new HashSet<>();
        this.store.keySet().stream()
                .filter(filter::test)
                .forEach(clientId -> {
                    this.store.remove(clientId);
                    removedClients.add(clientId);
                });
        return removedClients;
    }

    public Set<String> cleanUp(long tombstoneMaxAge) {
        Set<String> cleanedClients = new HashSet<>();
        this.store.entrySet().forEach(entry -> {
            String clientId = entry.getKey();
            TimestampObject<Set<Topic>> timestampObject = entry.getValue();
            if (timestampObject == null) {
                cleanedClients.add(clientId);
            } else if (timestampObject.getObject() == null &&
                    System.currentTimeMillis() - timestampObject.getTimestamp() > tombstoneMaxAge) {
                this.store.remove(clientId);
                cleanedClients.add(clientId);
            }
        });
        return cleanedClients;
    }

    public Map<String, TimestampObject<Set<Topic>>> getEntries(@NotNull Filter filter) {
        Preconditions.checkNotNull(filter, "Filter must not be null");
        Map<String, TimestampObject<Set<Topic>>> entries = new HashMap<>();
        this.store.entrySet().stream()
                .filter(entry -> filter.test(entry.getKey()))
                .forEach(entry -> entries.put(entry.getKey(), entry.getValue()));
        return entries;
    }

    public void close() {
    }

    public void addSubscription(@NotNull String clientId, @NotNull Topic topic, long timestamp) {
        Preconditions.checkNotNull(clientId, "Client must not be null");
        Preconditions.checkNotNull(topic, "Topic must not be null");
        Preconditions.checkArgument(topic.getQoS() != null, "Topic QoS must not be null");
        TimestampObject<Set<Topic>> timestampObject = this.store.get(clientId);
        Set<Topic> subscriptions;
        if (timestampObject == null) {
            subscriptions = new HashSet<>();
        } else {
            subscriptions = timestampObject.getObject();
        }
        if (!subscriptions.add(topic)) {
            subscriptions.remove(topic);
            subscriptions.add(topic);
        }
        this.store.put(clientId, new TimestampObject(subscriptions, timestamp));
    }

    public ImmutableSet<Topic> getSubscriptions(@NotNull String clientId) {
        Preconditions.checkNotNull(clientId, "Client must not be null");
        TimestampObject<Set<Topic>> timestampObject = this.store.get(clientId);
        if (timestampObject != null) {
            return ImmutableSet.copyOf(timestampObject.getObject());
        }
        return ImmutableSet.of();
    }

    public void removeAllSubscriptions(@NotNull String clientId, long timestamp) {
        Preconditions.checkNotNull(clientId, "Client must not be null");
        this.store.put(clientId, new TimestampObject(Sets.newHashSet(), timestamp));
    }

    public void removeSubscription(@NotNull String clientId, @NotNull Topic topic, long timestamp) {
        Preconditions.checkNotNull(clientId, "Client must not be null");
        Preconditions.checkNotNull(topic, "Topic must not be null");
        TimestampObject<Set<Topic>> timestampObject = this.store.get(clientId);
        if (timestampObject != null) {
            Set<Topic> subscriptions = timestampObject.getObject();
            subscriptions.remove(topic);
            this.store.put(clientId, new TimestampObject(subscriptions, timestamp));
        }
    }

    @Nullable
    public Long getTimestamp(@NotNull String clientId) {
        Preconditions.checkNotNull(clientId, "Client id must not be null");
        TimestampObject timestampObject = this.store.get(clientId);
        if (timestampObject == null) {
            return null;
        }
        return timestampObject.getTimestamp();
    }
}
