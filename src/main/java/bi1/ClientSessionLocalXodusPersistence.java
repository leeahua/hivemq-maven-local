package bi1;

import av.InternalConfigurationService;
import av.Internals;
import av.PersistenceConfigurationService;
import bc1.ClientSessionLocalPersistence;
import bd1.PersistenceFolders;
import bg1.XodusUtils;
import bh1.Bucket;
import bh1.BucketUtils;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.hivemq.spi.annotations.NotNull;
import com.hivemq.spi.annotations.ThreadSafe;
import com.hivemq.spi.exceptions.UnrecoverableException;
import d.CacheScoped;
import jetbrains.exodus.ByteIterable;
import jetbrains.exodus.ExodusException;
import jetbrains.exodus.env.Cursor;
import jetbrains.exodus.env.Environment;
import jetbrains.exodus.env.EnvironmentConfig;
import jetbrains.exodus.env.Environments;
import jetbrains.exodus.env.Store;
import jetbrains.exodus.env.StoreConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import u.Filter;
import u.TimestampObject;
import v.ClientSession;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.io.File;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@ThreadSafe
@CacheScoped
public class ClientSessionLocalXodusPersistence implements ClientSessionLocalPersistence {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientSessionLocalXodusPersistence.class);
    private static final String ENVIRONMENT_NAME = "client_session_store";
    public static final String CURRENT_VERSION = "030100";
    private final PersistenceFolders persistenceFolders;
    private final PersistenceConfigurationService persistenceConfigurationService;
    private final InternalConfigurationService internalConfigurationService;
    private final AtomicInteger persistentSessionSize = new AtomicInteger(0);
    private ClientSessionSerializer serializer;
    private ConcurrentHashMap<Integer, Bucket> buckets;
    private int bucketCount;
    private boolean initialized = false;

    @Inject
    ClientSessionLocalXodusPersistence(PersistenceFolders persistenceFolders,
                                       PersistenceConfigurationService persistenceConfigurationService,
                                       InternalConfigurationService internalConfigurationService) {
        this.persistenceFolders = persistenceFolders;
        this.persistenceConfigurationService = persistenceConfigurationService;
        this.internalConfigurationService = internalConfigurationService;
    }

    @PostConstruct
    void init() {
        if (this.initialized) {
            return;
        }
        this.bucketCount = this.internalConfigurationService.getInt(Internals.PERSISTENCE_CLIENT_SESSIONS_BUCKET_COUNT);
        this.serializer = new ClientSessionSerializer();
        try {
            EnvironmentConfig config = XodusUtils.buildConfig(this.persistenceConfigurationService.getClientSessionGeneralConfig(), ENVIRONMENT_NAME);
            this.buckets = new ConcurrentHashMap<>();
            for (int bucket = 0; bucket < this.bucketCount; bucket++) {
                Environment environment = Environments.newInstance(
                        new File(this.persistenceFolders.create("client_sessions", "030100"),
                                "client_session_store_" + bucket), config);
                Store store = environment.computeInTransaction(txn -> {
                    LOGGER.trace("Opening Persistent Session store");
                    return environment.openStore(ENVIRONMENT_NAME, StoreConfig.WITHOUT_DUPLICATES, txn);
                });
                this.buckets.put(bucket, new Bucket(environment, store));
            }
        } catch (ExodusException e) {
            LOGGER.error("An error occurred while opening the Xodus Client Session persistence. Is another HiveMQ instance running?");
            LOGGER.debug("Original Exception:", e);
            throw new UnrecoverableException(false);
        }
        initPersistentSessionSize();
        this.initialized = true;
    }

    private void initPersistentSessionSize() {
        AtomicInteger size = new AtomicInteger(0);
        this.buckets.values().forEach(bucket ->
                bucket.getEnvironment().executeInReadonlyTransaction(txn -> {
                    Cursor cursor = bucket.getStore().openCursor(txn);
                    while (cursor.getNext()) {
                        ClientSession clientSession = serializer.deserialize(XodusUtils.toBytes(cursor.getValue()));
                        if (clientSession != null && clientSession.isPersistentSession()) {
                            size.incrementAndGet();
                        }
                    }
                }));
        this.persistentSessionSize.set(size.get());
    }

    public ClientSession get(@NotNull String clientId) {
        Preconditions.checkNotNull(clientId, "Client id must not be null");
        Bucket bucket = getBucket(clientId);
        return bucket.getEnvironment().computeInReadonlyTransaction(txn -> {
            ByteIterable foundValue = bucket.getStore()
                    .get(txn, XodusUtils.toByteIterable(serializer.serializeKey(clientId)));
            if (foundValue == null) {
                return null;
            }
            return serializer.deserialize(XodusUtils.toBytes(foundValue));
        });
    }

    public void persistent(@NotNull String clientId, @NotNull ClientSession clientSession, long timestamp) {
        Preconditions.checkNotNull(clientId, "Client id must not be null");
        Preconditions.checkNotNull(clientSession, "Client session must not be null");
        Preconditions.checkArgument(timestamp > 0L, "Timestamp must be greater than 0");
        Bucket bucket = getBucket(clientId);
        bucket.getEnvironment().executeInTransaction(txn -> {
            ByteIterable searchKey = XodusUtils.toByteIterable(serializer.serializeKey(clientId));
            ByteIterable foundValue = bucket.getStore().get(txn, searchKey);
            if (foundValue != null) {
                ClientSession oldClientSession = serializer.deserialize(XodusUtils.toBytes(foundValue));
                if (clientSession.isPersistentSession() &&
                        oldClientSession != null &&
                        !oldClientSession.isPersistentSession()) {
                    persistentSessionSize.incrementAndGet();
                } else if (!clientSession.isPersistentSession() &&
                        oldClientSession != null &&
                        oldClientSession.isPersistentSession()) {
                    persistentSessionSize.decrementAndGet();
                }
            } else if (clientSession.isPersistentSession()) {
                persistentSessionSize.incrementAndGet();
            }
            bucket.getStore().put(txn, searchKey, XodusUtils.toByteIterable(serializer.serialize(clientSession, timestamp)));
        });
    }

    public ImmutableMap<String, ClientSession> disconnectAll(@NotNull String node, int bucketIndex) {
        Preconditions.checkNotNull(node, "Node id must not be null");
        Preconditions.checkArgument(bucketIndex >= 0 && bucketIndex < this.bucketCount, "Bucket index out of range");
        long timestamp = System.currentTimeMillis();
        Bucket bucket = this.buckets.get(bucketIndex);
        return bucket.getEnvironment().computeInTransaction(txn -> {
            ImmutableMap.Builder<String, ClientSession> builder = ImmutableMap.builder();
            Cursor cursor = bucket.getStore().openCursor(txn);
            while (cursor.getNext()) {
                ClientSession clientSession = serializer.deserialize(XodusUtils.toBytes(cursor.getValue()));
                if (clientSession.getConnectedNode().equals(node)) {
                    clientSession.setConnected(false);
                    bucket.getStore().put(txn, cursor.getKey(),
                            XodusUtils.toByteIterable(serializer.serialize(clientSession, timestamp)));
                    builder.put(serializer.deserializeKey(XodusUtils.toBytes(cursor.getKey())), clientSession);
                }
            }
            return builder.build();
        });
    }

    public void remove(@NotNull String clientId) {
        Preconditions.checkNotNull(clientId, "Client id must not be null");
        Bucket bucket = getBucket(clientId);
        bucket.getEnvironment().executeInTransaction(txn -> {
            ByteIterable searchKey = XodusUtils.toByteIterable(serializer.serializeKey(clientId));
            ByteIterable foundValue = bucket.getStore().get(txn, searchKey);
            if (foundValue != null) {
                ClientSession clientSession = serializer.deserialize(XodusUtils.toBytes(foundValue));
                if (clientSession != null && clientSession.isPersistentSession()) {
                    persistentSessionSize.decrementAndGet();
                }
            }
            bucket.getStore().delete(txn, searchKey);
        });
    }

    public ClientSession disconnect(@NotNull String clientId, @NotNull String connectedNode, long timestamp) {
        Preconditions.checkNotNull(clientId, "Client id must not be null");
        Preconditions.checkNotNull(connectedNode, "Connected node must not be null");
        Bucket bucket = getBucket(clientId);
        return bucket.getEnvironment().computeInTransaction(txn -> {
            ByteIterable searchKey = XodusUtils.toByteIterable(serializer.serializeKey(clientId));
            ByteIterable foundValue = bucket.getStore().get(txn, searchKey);
            if (foundValue == null) {
                ClientSession clientSession = new ClientSession(false, false, connectedNode);
                bucket.getStore().put(txn, searchKey,
                        XodusUtils.toByteIterable(serializer.serialize(clientSession, timestamp)));
                return clientSession;
            }
            ClientSession clientSession = serializer.deserialize(XodusUtils.toBytes(foundValue));
            if (connectedNode.equals(clientSession.getConnectedNode())) {
                clientSession.setConnected(false);
                clientSession.setConnectedNode(connectedNode);
                bucket.getStore().put(txn, searchKey,
                        XodusUtils.toByteIterable(serializer.serialize(clientSession, timestamp)));
            }
            return clientSession;
        });
    }

    public Map<String, TimestampObject<ClientSession>> get(@NotNull Filter filter, int bucketIndex) {
        Preconditions.checkNotNull(filter, "Filter must not be null");
        Preconditions.checkArgument(bucketIndex >= 0 && bucketIndex < this.bucketCount, "Bucket index out of range");
        Bucket bucket = this.buckets.get(bucketIndex);
        return bucket.getEnvironment().computeInTransaction(txn -> {
            Map<String, TimestampObject<ClientSession>> results = Maps.newHashMap();
            Cursor cursor = bucket.getStore().openCursor(txn);
            while (cursor.getNext()) {
                String clientId = serializer.deserializeKey(XodusUtils.toBytes(cursor.getKey()));
                if (filter.test(clientId)) {
                    byte[] valueBytes = XodusUtils.toBytes(cursor.getValue());
                    results.put(clientId, new TimestampObject<>(serializer.deserialize(valueBytes), serializer.deserializeTimestamp(valueBytes)));
                }
            }
            return results;
        });
    }

    public ImmutableSet<String> remove(Filter filter, int bucketIndex) {
        ImmutableSet.Builder<String> builder = ImmutableSet.builder();
        Bucket bucket = this.buckets.get(bucketIndex);
        bucket.getEnvironment().executeInTransaction(txn -> {
            Cursor cursor = bucket.getStore().openCursor(txn);
            while (cursor.getNext()) {
                String clientId = serializer.deserializeKey(XodusUtils.toBytes(cursor.getKey()));
                if (filter.test(clientId)) {
                    builder.add(clientId);
                    ClientSession clientSession = serializer.deserialize(XodusUtils.toBytes(cursor.getValue()));
                    if (clientSession != null && clientSession.isPersistentSession()) {
                        persistentSessionSize.decrementAndGet();
                    }
                    cursor.deleteCurrent();
                }
            }
        });
        return builder.build();
    }

    public void merge(@NotNull String clientId, @NotNull ClientSession clientSession, long timestamp) {
        Bucket bucket = getBucket(clientId);
        bucket.getEnvironment().executeInTransaction(txn -> {
            ByteIterable searchKey = XodusUtils.toByteIterable(serializer.serializeKey(clientId));
            ByteIterable foundValue = bucket.getStore().get(txn, searchKey);
            if (foundValue == null) {
                bucket.getStore().put(txn, searchKey,
                        XodusUtils.toByteIterable(serializer.serialize(clientSession, timestamp)));
                if (clientSession.isPersistentSession()) {
                    persistentSessionSize.incrementAndGet();
                }
                return;
            }
            ClientSession oldClientSession = serializer.deserialize(XodusUtils.toBytes(foundValue));
            if (oldClientSession.isConnected()) {
                return;
            }
            bucket.getStore().put(txn, searchKey,
                    XodusUtils.toByteIterable(serializer.serialize(clientSession, timestamp)));
            if (clientSession.isPersistentSession() &&
                    !oldClientSession.isPersistentSession()) {
                persistentSessionSize.incrementAndGet();
            } else if (!clientSession.isPersistentSession() &&
                    oldClientSession.isPersistentSession()) {
                persistentSessionSize.decrementAndGet();
            }
        });
    }

    public Set<String> cleanUp(long tombstoneMaxAge, int bucketIndex) {
        ImmutableSet.Builder<String> builder = ImmutableSet.builder();
        Bucket bucket = this.buckets.get(bucketIndex);
        bucket.getEnvironment().executeInTransaction(txn -> {
            Cursor cursor = bucket.getStore().openCursor(txn);
            while (cursor.getNext()) {
                String clientId = serializer.deserializeKey(XodusUtils.toBytes(cursor.getKey()));
                byte[] valueBytes = XodusUtils.toBytes(cursor.getValue());
                ClientSession clientSession = serializer.deserialize(valueBytes);
                long timestamp = serializer.deserializeTimestamp(valueBytes);
                boolean cleanSession = clientSession == null ||
                        (!clientSession.isConnected() && !clientSession.isPersistentSession());
                if (cleanSession && System.currentTimeMillis() - timestamp > tombstoneMaxAge) {
                    builder.add(clientId);
                    cursor.deleteCurrent();
                }
            }
        });
        return builder.build();
    }

    public int persistentSessionSize() {
        return this.persistentSessionSize.get();
    }

    public void close() {
        this.buckets.values().stream()
                .filter(bucket -> bucket.getEnvironment().isOpen())
                .forEach(bucket -> bucket.getEnvironment().close());
    }

    private Bucket getBucket(String clientId) {
        return this.buckets.get(BucketUtils.bucket(clientId, this.bucketCount));
    }

    public int getBucketCount() {
        return this.bucketCount;
    }
}
