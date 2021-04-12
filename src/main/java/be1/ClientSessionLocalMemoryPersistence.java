package be1;

import bc1.ClientSessionLocalPersistence;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.hivemq.spi.annotations.NotNull;
import com.hivemq.spi.annotations.Nullable;
import com.hivemq.spi.annotations.ThreadSafe;
import d.CacheScoped;
import u.Filter;
import u.TimestampObject;
import v.ClientSession;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@ThreadSafe
@CacheScoped
public class ClientSessionLocalMemoryPersistence implements ClientSessionLocalPersistence {
    private final ConcurrentHashMap<String, TimestampObject<ClientSession>> store = new ConcurrentHashMap<>();
    private final AtomicInteger persistentSessionSize = new AtomicInteger(0);

    @Nullable
    public ClientSession get(@NotNull String clientId) {
        Preconditions.checkNotNull(clientId, "Client id must not be null");
        TimestampObject<ClientSession> timestampObject = this.store.get(clientId);
        if (timestampObject == null) {
            return null;
        }
        return timestampObject.getObject();
    }

    public ClientSession disconnect(@NotNull String clientId, @NotNull String connectedNode, long timestamp) {
        Preconditions.checkNotNull(clientId, "Client id must not be null");
        Preconditions.checkNotNull(connectedNode, "Connected node must not be null");
        TimestampObject<ClientSession> timestampObject = this.store.get(clientId);
        if (timestampObject == null) {
            return null;
        }
        ClientSession clientSession = timestampObject.getObject();
        if (!clientSession.getConnectedNode().equals(connectedNode)) {
            return clientSession;
        }
        ClientSession newClientSession = new ClientSession(false, clientSession.isPersistentSession(), connectedNode);
        this.store.put(clientId, new TimestampObject<>(newClientSession, timestamp));
        return newClientSession;
    }

    public Map<String, TimestampObject<ClientSession>> get(@NotNull Filter filter, int bucketIndex) {
        Preconditions.checkNotNull(filter, "Filter must not be null");
        ImmutableMap.Builder<String, TimestampObject<ClientSession>> builder = ImmutableMap.builder();
        this.store.entrySet().stream()
                .filter(entry -> filter.test(entry.getKey()))
                .filter(entry -> Objects.nonNull(entry.getValue()))
                .forEach(builder::put);
        return builder.build();
    }

    public ImmutableSet<String> remove(@NotNull Filter filter, int bucketIndex) {
        Preconditions.checkNotNull(filter, "Filter must not be null");
        ImmutableSet.Builder<String> builder = ImmutableSet.builder();
        this.store.entrySet().stream()
                .filter(entry -> filter.test(entry.getKey()))
                .forEach(entry -> {
                    String clientId = entry.getKey();
                    TimestampObject<ClientSession> timestampObject = entry.getValue();
                    if (timestampObject.getObject() != null &&
                            timestampObject.getObject().isPersistentSession()) {
                        this.persistentSessionSize.decrementAndGet();
                    }
                    this.store.remove(clientId);
                    builder.add(clientId);
                });
        return builder.build();
    }

    public void persistent(@NotNull String clientId, @NotNull ClientSession clientSession, long timestamp) {
        Preconditions.checkNotNull(clientId, "Client id must not be null");
        Preconditions.checkNotNull(clientSession, "Client session must not be null");
        TimestampObject<ClientSession> timestampObject = this.store.get(clientId);
        if (clientSession.isPersistentSession() &&
                (timestampObject == null ||
                        timestampObject.getObject() == null ||
                        !timestampObject.getObject().isPersistentSession())) {
            this.persistentSessionSize.incrementAndGet();
        } else if (!clientSession.isPersistentSession() &&
                timestampObject != null &&
                timestampObject.getObject() != null &&
                timestampObject.getObject().isPersistentSession()) {
            this.persistentSessionSize.decrementAndGet();
        }
        this.store.put(clientId, new TimestampObject(clientSession, timestamp));
    }

    public void merge(@NotNull String clientId, @NotNull ClientSession clientSession, long timestamp) {
        Preconditions.checkNotNull(clientId, "Client id must not be null");
        Preconditions.checkNotNull(clientSession, "Client session must not be null");
        TimestampObject<ClientSession> timestampObject = this.store.get(clientId);
        if (timestampObject == null || !timestampObject.getObject().isConnected()) {
            if (clientSession.isPersistentSession()) {
                if (timestampObject == null ||
                        timestampObject.getObject() == null ||
                        !timestampObject.getObject().isPersistentSession()) {
                    this.persistentSessionSize.incrementAndGet();
                }
            } else if (timestampObject != null &&
                    timestampObject.getObject() != null &&
                    timestampObject.getObject().isPersistentSession()) {
                this.persistentSessionSize.decrementAndGet();
            }
            this.store.put(clientId, new TimestampObject(clientSession, timestamp));
        }
    }

    public ImmutableMap<String, ClientSession> disconnectAll(@NotNull String node, int bucketIndex) {
        Preconditions.checkNotNull(node, "Node id must not be null");
        ImmutableMap.Builder<String, ClientSession> builder = ImmutableMap.builder();
        this.store.entrySet().stream()
                .filter(entry -> Objects.nonNull(entry.getValue()))
                .filter(entry -> entry.getValue().getObject() != null &&
                        entry.getValue().getObject().getConnectedNode().equals(node))
                .forEach(entry -> {
                    entry.getValue().getObject().setConnected(false);
                    builder.put(entry.getKey(), entry.getValue().getObject());
                });
        return builder.build();
    }

    public void remove(@NotNull String clientId) {
        Preconditions.checkNotNull(clientId, "Client id must not be null");
        TimestampObject<ClientSession> timestampObject = this.store.get(clientId);
        if (timestampObject != null &&
                timestampObject.getObject() != null &&
                timestampObject.getObject().isPersistentSession()) {
            this.persistentSessionSize.decrementAndGet();
        }
        this.store.remove(clientId);
    }

    public Set<String> cleanUp(long tombstoneMaxAge, int bucketIndex) {
        ImmutableSet.Builder<String> builder = ImmutableSet.builder();
        this.store.forEach((clientId, timestampObject) -> {
            if (timestampObject == null) {
                builder.add(clientId);
                return;
            }
            long timestamp = timestampObject.getTimestamp();
            ClientSession clientSession = timestampObject.getObject();
            boolean disconnectedAndCleanSession = clientSession == null ||
                    (!clientSession.isConnected() && !clientSession.isPersistentSession());
            if (disconnectedAndCleanSession && System.currentTimeMillis() - timestamp > tombstoneMaxAge) {
                this.store.remove(clientId);
                builder.add(clientId);
            }
        });
        return builder.build();
    }

    public int persistentSessionSize() {
        return this.persistentSessionSize.get();
    }

    public void close() {
    }
}
