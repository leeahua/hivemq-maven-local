package ay1;

import av.PersistenceConfigurationService;
import bd1.PersistenceFolders;
import bg1.XodusUtils;
import bh1.BucketUtils;
import bh1.LockBucket;
import bi1.ClientSessionLocalXodusPersistence;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.hivemq.spi.annotations.NotNull;
import com.hivemq.spi.annotations.Nullable;
import com.hivemq.spi.annotations.ReadOnly;
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

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.io.File;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@ThreadSafe
@CacheScoped
public class ClientSessionLocalXodusV030003Persistence {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientSessionLocalXodusPersistence.class);
    private static final String ENVIRONMENT_NAME = "client_session_store";
    private static final int BUCKET_COUNT = 8;
    private final EntrySerializer serializer = new EntrySerializer();
    private final PersistenceFolders persistenceFolders;
    private final PersistenceConfigurationService persistenceConfigurationService;
    private ConcurrentHashMap<Integer, LockBucket> buckets;

    @Inject
    ClientSessionLocalXodusV030003Persistence(
            PersistenceFolders persistenceFolders,
            PersistenceConfigurationService persistenceConfigurationService) {
        this.persistenceFolders = persistenceFolders;
        this.persistenceConfigurationService = persistenceConfigurationService;
    }

    @PostConstruct
    public void init() {
        try {
            EnvironmentConfig config = XodusUtils.buildConfig(
                    this.persistenceConfigurationService.getClientSessionGeneralConfig(), ENVIRONMENT_NAME);
            this.buckets = new ConcurrentHashMap<>();
            for (int bucket = 0; bucket < BUCKET_COUNT; bucket++) {
                Environment environment = Environments.newInstance(
                        new File(this.persistenceFolders.root(), "client_session_store_" + bucket), config);
                Store store = environment.computeInTransaction(txn -> {
                    LOGGER.trace("Opening Persistent Session store");
                    return environment.openStore(ENVIRONMENT_NAME, StoreConfig.WITHOUT_DUPLICATES, txn);
                });
                this.buckets.put(bucket, new LockBucket(environment, store, new ReentrantReadWriteLock(true)));
            }
        } catch (ExodusException e) {
            LOGGER.error("An error occurred while opening the Xodus Client Session persistence. Is another HiveMQ instance running?");
            LOGGER.debug("Original Exception:", e);
            throw new UnrecoverableException(false);
        }
    }

    public void migrate(Callback callback) {
        this.buckets.values().forEach(bucket -> {
            Lock lock = bucket.getLock().readLock();
            lock.lock();
            try {
                bucket.getEnvironment().executeInReadonlyTransaction(txn -> {
                    Cursor cursor = bucket.getStore().openCursor(txn);
                    Throwable throwable = null;
                    try {
                        while (cursor.getNext()) {
                            String clientId = XodusUtils.toString(cursor.getKey());
                            long timestamp = serializer.deserialize(XodusUtils.toBytes(cursor.getValue()));
                            callback.persistent(clientId, timestamp);
                        }
                    } catch (Throwable e) {
                        throwable = e;
                        throw e;
                    } finally {
                        XodusUtils.close(cursor, throwable);
                    }
                });
            } finally {
                lock.unlock();
            }
        });
    }

    @Nullable
    @ThreadSafe
    public Long get(@NotNull String clientId) {
        Preconditions.checkNotNull(clientId, "Client id must not be null");
        LockBucket bucket = this.buckets.get(BucketUtils.bucket(clientId, BUCKET_COUNT));
        Lock lock = bucket.getLock().readLock();
        lock.lock();
        try {
            return bucket.getEnvironment().computeInReadonlyTransaction(txn -> {
                ByteIterable foundValue = bucket.getStore().get(txn, XodusUtils.toByteIterable(clientId));
                if (foundValue != null) {
                    long timestamp = serializer.deserialize(XodusUtils.toBytes(foundValue));
                    LOGGER.trace("Client with id {} found in persistent session store with timestamp {}",
                            clientId, timestamp);
                    return timestamp;
                }
                LOGGER.trace("Client with id {} not found in persistent session store", clientId);
                return null;
            });
        } finally {
            lock.unlock();
        }
    }

    @ThreadSafe
    public void persistent(@NotNull String clientId, long timestamp) {
        Preconditions.checkNotNull(clientId, "Client id must not be null");
        Preconditions.checkArgument(timestamp > 0L, "Timestamp must be higher than 0");
        LockBucket bucket = this.buckets.get(BucketUtils.bucket(clientId, BUCKET_COUNT));
        Lock lock = bucket.getLock().writeLock();
        lock.lock();
        try {
            bucket.getEnvironment().executeInTransaction(txn -> {
                LOGGER.trace("Persisting client with id {} and timestamp {} to persistent session store",
                        clientId, timestamp);
                bucket.getStore().put(txn, XodusUtils.toByteIterable(clientId),
                        XodusUtils.toByteIterable(serializer.serialize(timestamp)));
            });
        } finally {
            lock.unlock();
        }
    }

    @Nullable
    @ThreadSafe
    public Long remove(@NotNull String clientId) {
        Preconditions.checkNotNull(clientId, "Client id must not be null");
        LockBucket bucket = this.buckets.get(BucketUtils.bucket(clientId, BUCKET_COUNT));
        Lock lock = bucket.getLock().writeLock();
        lock.lock();
        try {
            return bucket.getEnvironment().computeInTransaction(txn -> {
                ByteIterable searchKey = XodusUtils.toByteIterable(clientId);
                ByteIterable foundValue = bucket.getStore().get(txn, searchKey);
                if (foundValue != null) {
                    LOGGER.trace("Deleting client with id {} from persistent session store", clientId);
                    bucket.getStore().delete(txn, searchKey);
                    return serializer.deserialize(XodusUtils.toBytes(foundValue));
                }
                LOGGER.trace("Could not delete client with id {} from persistent session store: Not found", clientId);
                return null;
            });
        } finally {
            lock.unlock();
        }
    }

    @ThreadSafe
    public long size() {
        return this.buckets.values().stream()
                .mapToLong(bucket -> {
                    Lock lock = bucket.getLock().readLock();
                    lock.lock();
                    try {
                        return bucket.getEnvironment()
                                .computeInReadonlyTransaction(txn -> bucket.getStore().count(txn));
                    } finally {
                        lock.unlock();
                    }
                }).sum();
    }

    @ThreadSafe
    public boolean isEmpty() {
        return size() == 0L;
    }

    @ThreadSafe
    public void clear() {
        this.buckets.values().forEach(
                bucket -> bucket.getLock().writeLock().lock());
        try {
            this.buckets.values().forEach(bucket ->
                    bucket.getEnvironment().executeInTransaction(txn -> {
                        LOGGER.debug("Clearing persistent session store");
                        bucket.getEnvironment().removeStore(ENVIRONMENT_NAME, txn);
                    })
            );
        } finally {
            this.buckets.values().forEach(
                    bucket -> bucket.getLock().writeLock().unlock());
        }
    }

    @ThreadSafe
    public boolean contains(@NotNull String clientId) {
        Preconditions.checkNotNull(clientId, "Client id must not be null");
        Long timestamp = get(clientId);
        return timestamp != null;
    }

    @ThreadSafe
    @ReadOnly
    public Set<String> getPersistentClients() {
        ImmutableSet.Builder<String> builder = ImmutableSet.builder();
        this.buckets.values().forEach(bucket -> {
            Lock lock = bucket.getLock().readLock();
            lock.lock();
            try {
                builder.addAll(bucket.getEnvironment().computeInReadonlyTransaction(txn -> {
                    ImmutableSet.Builder<String> bucketBuilder = ImmutableSet.builder();
                    Cursor cursor = bucket.getStore().openCursor(txn);
                    Throwable throwable = null;
                    try {
                        while (cursor.getNext()) {
                            bucketBuilder.add(XodusUtils.toString(cursor.getKey()));
                        }
                    } catch (Throwable e) {
                        throwable = e;
                        throw e;
                    } finally {
                        XodusUtils.close(cursor, throwable);
                    }
                    return bucketBuilder.build();
                }));
            } finally {
                lock.unlock();
            }

        });
        return builder.build();
    }

    public void close() {
        this.buckets.values().forEach(
                bucket -> bucket.getLock().writeLock().lock());
        try {
            this.buckets.values().stream()
                    .filter(bucket -> bucket.getEnvironment().isOpen())
                    .forEach(bucket -> bucket.getEnvironment().close());
        } finally {
            this.buckets.values().forEach(
                    bucket -> bucket.getLock().writeLock().unlock());
        }
    }

    public interface Callback {
        void persistent(String clientId, Long timestamp);
    }

    private class EntrySerializer {
        private EntrySerializer() {
        }

        public long deserialize(@NotNull byte[] data) {
            return (data[5] & 0xFF) << 40 |
                    (data[4] & 0xFF) << 32 |
                    (data[3] & 0xFF) << 24 |
                    (data[2] & 0xFF) << 16 |
                    (data[1] & 0xFF) << 8 |
                    data[0] & 0xFF;
        }

        @NotNull
        public byte[] serialize(long timestamp) {
            return new byte[]{
                    (byte) (int) timestamp,
                    (byte) (int) (timestamp >> 8),
                    (byte) (int) (timestamp >> 16),
                    (byte) (int) (timestamp >> 24),
                    (byte) (int) (timestamp >> 32),
                    (byte) (int) (timestamp >> 40)
            };
        }
    }
}
