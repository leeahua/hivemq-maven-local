package be1;

import av.PersistenceConfigurationService;
import bc1.ClientSessionQueueEntry;
import bc1.QueuedMessagesLocalPersistence;
import bf1.ClientSessionQueue;
import bf1.ClientSessionQueueFactory;
import bu.InternalPublish;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.hivemq.spi.annotations.NotNull;
import com.hivemq.spi.annotations.Nullable;
import com.hivemq.spi.annotations.ThreadSafe;
import d.CacheScoped;
import i.ClusterConfigurationService;
import u.Filter;
import u.TimestampObject;

import javax.inject.Inject;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@ThreadSafe
@CacheScoped
public class QueuedMessagesLocalMemoryPersistence implements QueuedMessagesLocalPersistence {
    private final ConcurrentHashMap<String, TimestampObject<ClientSessionQueue>> store;
    private final PersistenceConfigurationService persistenceConfigurationService;
    private final ClusterConfigurationService clusterConfigurationService;

    @Inject
    QueuedMessagesLocalMemoryPersistence(
            PersistenceConfigurationService persistenceConfigurationService,
            ClusterConfigurationService clusterConfigurationService) {
        this.persistenceConfigurationService = persistenceConfigurationService;
        this.clusterConfigurationService = clusterConfigurationService;
        this.store = new ConcurrentHashMap<>();
    }

    public void clear() {
        this.store.clear();
    }

    public void close() {
        clear();
    }

    public void offer(@NotNull String clientId, @NotNull InternalPublish publish, long timestamp) {
        ClientSessionQueue queue = getClientSessionQueue(clientId, timestamp);
        queue.offer(new ClientSessionQueueEntry(publish.getSequence(),
                publish.getTimestamp(),
                publish.getTopic(),
                publish.getQoS(),
                publish.getPayload(),
                publish.getClusterId()));
        this.store.put(clientId, new TimestampObject(queue, timestamp));
    }

    public boolean queuePublishIfQueueNotEmpty(@NotNull String clientId, @NotNull InternalPublish publish) {
        TimestampObject<ClientSessionQueue> timestampObject = this.store.get(clientId);
        if (timestampObject == null) {
            return false;
        }
        ClientSessionQueue clientSessionQueue = timestampObject.getObject();
        if (clientSessionQueue == null || clientSessionQueue.size() == 0L) {
            return false;
        }
        offer(clientId, publish, System.currentTimeMillis());
        return true;
    }

    @NotNull
    private ClientSessionQueue getClientSessionQueue(@NotNull String clientId, long timestamp) {
        TimestampObject<ClientSessionQueue> timestampObject = this.store.get(clientId);
        if (timestampObject == null || timestampObject.getObject() == null) {
            ClientSessionQueue clientSessionQueue = ClientSessionQueueFactory.create(
                    this.persistenceConfigurationService.getMaxQueuedMessages(),
                    this.persistenceConfigurationService.getQueuedMessagesStrategy(),
                    this.clusterConfigurationService.isEnabled());
            this.store.put(clientId, new TimestampObject<>(clientSessionQueue, timestamp));
            return clientSessionQueue;
        }
        this.store.put(clientId, new TimestampObject<>(timestampObject.getObject(), timestamp));
        return timestampObject.getObject();
    }

    @Nullable
    public ClientSessionQueueEntry poll(@NotNull String clientId, long timestamp) {
        TimestampObject<ClientSessionQueue> timestampObject = this.store.get(clientId);
        if (timestampObject == null) {
            return null;
        }
        timestampObject.setTimestamp(timestamp);
        ClientSessionQueue clientSessionQueue = timestampObject.getObject();
        if (clientSessionQueue == null) {
            return null;
        }
        ClientSessionQueueEntry entry = clientSessionQueue.poll();
        if (clientSessionQueue.size() == 0L) {
            this.store.put(clientId, new TimestampObject<>(null, timestamp));
        }
        return entry;
    }

    public ClientSessionQueueEntry peek(@NotNull String clientId) {
        TimestampObject<ClientSessionQueue> timestampObject = this.store.get(clientId);
        if (timestampObject == null) {
            return null;
        }
        ClientSessionQueue clientSessionQueue = timestampObject.getObject();
        if (clientSessionQueue == null) {
            return null;
        }
        return clientSessionQueue.peek();
    }

    public void remove(@NotNull String clientId, long timestamp) {
        this.store.put(clientId, new TimestampObject(null, timestamp));
    }

    public void remove(@NotNull String clientId, @NotNull String entryId, long entryTimestamp, long timestamp) {
        TimestampObject<ClientSessionQueue> timestampObject = this.store.get(clientId);
        if (timestampObject == null) {
            return;
        }
        timestampObject.setTimestamp(timestamp);
        ClientSessionQueue clientSessionQueue = timestampObject.getObject();
        if (clientSessionQueue == null) {
            return;
        }
        clientSessionQueue.remove(entryId, entryTimestamp);
        if (clientSessionQueue.size() == 0L) {
            this.store.put(clientId, new TimestampObject(null, timestamp));
        }
    }

    public long size(@NotNull String clientId) {
        TimestampObject<ClientSessionQueue> timestampObject = this.store.get(clientId);
        if (timestampObject == null) {
            return 0L;
        }
        ClientSessionQueue clientSessionQueue = timestampObject.getObject();
        if (clientSessionQueue == null) {
            return 0L;
        }
        return clientSessionQueue.size();
    }

    public ImmutableMap<String, TimestampObject<Set<ClientSessionQueueEntry>>> getEntries(Filter filter) {
        ImmutableMap.Builder<String, TimestampObject<Set<ClientSessionQueueEntry>>> builder = ImmutableMap.builder();
        this.store.entrySet().stream()
                .filter(entry -> filter.test(entry.getKey()))
                .forEach(entry -> {
                    TimestampObject<ClientSessionQueue> timestampObject = entry.getValue();
                    ClientSessionQueue clientSessionQueue = timestampObject.getObject();
                    if (clientSessionQueue == null) {
                        builder.put(entry.getKey(), new TimestampObject(Sets.newHashSet(), timestampObject.getTimestamp()));
                    } else {
                        builder.put(entry.getKey(), new TimestampObject(clientSessionQueue.getAll(), timestampObject.getTimestamp()));
                    }

                });
        return builder.build();
    }

    public void offerAll(ImmutableSet<ClientSessionQueueEntry> queueEntries, String clientId, long timestamp) {
        ClientSessionQueue clientSessionQueue = ClientSessionQueueFactory.create(
                this.persistenceConfigurationService.getMaxQueuedMessages(),
                this.persistenceConfigurationService.getQueuedMessagesStrategy(),
                this.clusterConfigurationService.isEnabled());
        queueEntries.forEach(clientSessionQueue::offer);
        this.store.put(clientId, new TimestampObject(clientSessionQueue, timestamp));
    }

    public long getTimestamp(String clientId) {
        return this.store.get(clientId).getTimestamp();
    }

    public ImmutableSet<String> remove(Filter filter) {
        ImmutableSet.Builder<String> builder = ImmutableSet.builder();
        this.store.keySet().stream()
                .filter(filter::test)
                .forEach(clientId -> {
                    builder.add(clientId);
                    this.store.remove(clientId);
                });
        return builder.build();
    }

    public ImmutableSet<String> cleanUp(long tombstoneMaxAge) {
        ImmutableSet.Builder<String> builder = ImmutableSet.builder();
        this.store.entrySet().forEach(entry -> {
            TimestampObject<ClientSessionQueue> timestampObject = entry.getValue();
            if (timestampObject == null) {
                builder.add(entry.getKey());
            } else if (timestampObject.getObject() == null &&
                    System.currentTimeMillis() - timestampObject.getTimestamp() > tombstoneMaxAge) {
                builder.add(entry.getKey());
                this.store.remove(entry.getKey());
            }
        });
        return builder.build();
    }
}
