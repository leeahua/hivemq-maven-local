package bg1;

import av.InternalConfigurationService;
import av.Internals;
import av.PersistenceConfigurationService;
import bd1.PersistenceFolders;
import bh1.Bucket;
import bh1.BucketUtils;
import bz.RetainedMessage;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.hivemq.spi.annotations.NotNull;
import com.hivemq.spi.annotations.Nullable;
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
import x.RetainedMessagesLocalPersistence;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.io.File;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@CacheScoped
public class RetainedMessagesLocalXodusPersistence implements RetainedMessagesLocalPersistence {
    private static final Logger LOGGER = LoggerFactory.getLogger(RetainedMessagesLocalXodusPersistence.class);
    public static final String ENVIRONMENT_NAME = "retained_messages";
    public static final String CURRENT_VERSION = "030100";
    private final PersistenceFolders persistenceFolders;
    private final PersistenceConfigurationService persistenceConfigurationService;
    private final InternalConfigurationService internalConfigurationService;
    private ConcurrentHashMap<Integer, Bucket> buckets;
    private RetainedMessageEntitySerializer serializer;
    private int bucketCount;
    private boolean initialized = false;

    @Inject
    public RetainedMessagesLocalXodusPersistence(PersistenceFolders persistenceFolders,
                                                 PersistenceConfigurationService persistenceConfigurationService,
                                                 InternalConfigurationService internalConfigurationService) {
        this.persistenceFolders = persistenceFolders;
        this.persistenceConfigurationService = persistenceConfigurationService;
        this.internalConfigurationService = internalConfigurationService;
    }

    @PostConstruct
    public void init() {
        if (this.initialized) {
            return;
        }
        this.bucketCount = this.internalConfigurationService.getInt(Internals.PERSISTENCE_RETAINED_MESSAGES_BUCKET_COUNT);
        try {
            this.serializer = new RetainedMessageEntitySerializer();
            EnvironmentConfig config = XodusUtils.buildConfig(this.persistenceConfigurationService.getRetainedMessagesConfig(), ENVIRONMENT_NAME);
            this.buckets = new ConcurrentHashMap<>();
            for (int bucket = 0; bucket < this.bucketCount; bucket++) {
                Environment environment = Environments.newInstance(
                        new File(this.persistenceFolders.create(ENVIRONMENT_NAME, CURRENT_VERSION),
                                "retained_messages_" + bucket), config);
                Store store = environment.computeInTransaction(txn ->
                        environment.openStore(ENVIRONMENT_NAME, StoreConfig.WITHOUT_DUPLICATES, txn));
                this.buckets.put(bucket, new Bucket(environment, store));
            }
        } catch (ExodusException e) {
            LOGGER.error("An error occurred while opening the Xodus Outgoing Messages Local persistence. Is another HiveMQ instance running?");
            LOGGER.debug("Original Exception:", e);
            throw new UnrecoverableException();
        }
        this.initialized = true;
    }

    public void clear(int bucketIndex) {
        Bucket bucket = this.buckets.get(bucketIndex);
        bucket.getEnvironment().executeInTransaction(txn -> {
            Cursor cursor = bucket.getStore().openCursor(txn);
            while (cursor.getNext()) {
                cursor.deleteCurrent();
            }
        });
    }

    public long size() {
        return this.buckets.values().stream()
                .mapToLong(bucket ->
                        bucket.getEnvironment()
                                .computeInReadonlyTransaction(txn -> {
                                    long size = 0;
                                    Cursor cursor = bucket.getStore().openCursor(txn);
                                    Throwable throwable = null;
                                    try {
                                        while (cursor.getNext()) {
                                            RetainedMessageEntity entity = serializer.deserializeEntry(XodusUtils.toBytes(cursor.getValue()));
                                            if (!entity.isDeleted()) {
                                                size++;
                                            }
                                        }
                                    } catch (Throwable e) {
                                        throwable = e;
                                        throw e;
                                    } finally {
                                        XodusUtils.close(cursor, throwable);
                                    }
                                    return size;
                                })
                ).sum();
    }

    public void remove(@NotNull String topic, long timestamp) {
        Preconditions.checkNotNull(topic, "Topic must not be null");
        Bucket bucket = this.buckets.get(BucketUtils.bucket(topic, this.bucketCount));
        bucket.getEnvironment().executeInTransaction(txn -> {
            ByteIterable searchKey = XodusUtils.toByteIterable(topic);
            ByteIterable foundValue = bucket.getStore().get(txn, searchKey);
            if (foundValue == null) {
                LOGGER.trace("Removing retained message for topic {} (no message was stored previously)", topic);
                bucket.getStore().put(txn, searchKey,
                        XodusUtils.toByteIterable(serializer.serializeTimestamp(timestamp)));
                return;
            }
            RetainedMessageEntity entity = serializer.deserializeEntry(XodusUtils.toBytes(foundValue));
            if (entity.getTimestamp() <= timestamp) {
                LOGGER.trace("Removing retained message for topic {}", topic);
                bucket.getStore().put(txn, searchKey,
                        XodusUtils.toByteIterable(serializer.serializeTimestamp(timestamp)));
            } else {
                LOGGER.trace("Retained message for topic {} was not removed because the persisted timestamp ({}) is newer than the passed message timestamp ({})",
                        topic, entity.getTimestamp(), timestamp);
            }
        });
    }

    @Nullable
    public RetainedMessage get(@NotNull String topic) {
        Preconditions.checkNotNull(topic, "Topic must not be null");
        Bucket bucket = this.buckets.get(BucketUtils.bucket(topic, this.bucketCount));
        return bucket.getEnvironment().computeInReadonlyTransaction(txn -> {
            ByteIterable foundValue = bucket.getStore().get(txn, XodusUtils.toByteIterable(serializer.serializeTopic(topic)));
            if (foundValue == null) {
                return null;
            }
            RetainedMessageEntity entity = serializer.deserializeEntry(XodusUtils.toBytes(foundValue));
            if (entity.isDeleted()) {
                return null;
            }
            return entity;
        });
    }

    @Nullable
    public Long getTimestamp(@NotNull String topic) {
        RetainedMessageEntity entity = (RetainedMessageEntity) get(topic);
        if (entity == null) {
            return null;
        }
        return entity.getTimestamp();
    }

    public void addOrReplace(@NotNull String topic, @NotNull RetainedMessage retainedMessage, long timestamp) {
        Preconditions.checkNotNull(topic, "Topic must not be null");
        Preconditions.checkNotNull(retainedMessage, "Retained message must not be null");
        Bucket bucket = this.buckets.get(BucketUtils.bucket(topic, this.bucketCount));
        bucket.getEnvironment().executeInTransaction(txn -> {
            Cursor cursor = bucket.getStore().openCursor(txn);
            Throwable throwable = null;
            try {
                ByteIterable searchKey = XodusUtils.toByteIterable(serializer.serializeTopic(topic));
                ByteIterable foundValue = cursor.getSearchKey(searchKey);
                if (foundValue != null) {
                    RetainedMessageEntity entity = serializer.deserializeEntry(XodusUtils.toBytes(cursor.getValue()));
                    if (entity.getTimestamp() <= timestamp) {
                        LOGGER.trace("Replacing retained message for topic {}", topic);
                        bucket.getStore().put(txn, searchKey,
                                XodusUtils.toByteIterable(serializer.serializeEntry(retainedMessage, timestamp)));
                    } else {
                        LOGGER.trace("Retained message for topic {} was not added because the persisted timestamp ({}) is newer than the passed message timestamp ({})",
                                topic, entity.getTimestamp(), timestamp);
                    }
                } else {
                    bucket.getStore().put(txn, searchKey,
                            XodusUtils.toByteIterable(serializer.serializeEntry(retainedMessage, timestamp)));
                    LOGGER.trace("Creating new retained message for topic {} and timestamp: {}",
                            topic, timestamp);
                }
            } catch (Throwable e) {
                throwable = e;
                throw e;
            } finally {
                XodusUtils.close(cursor, throwable);
            }
        });
    }

    public Map<String, TimestampObject<RetainedMessage>> getEntries(@NotNull Filter filter, int bucketIndex) {
        Preconditions.checkArgument(bucketIndex >= 0 && bucketIndex < this.bucketCount, "Bucket index out of range");
        Bucket bucket = this.buckets.get(bucketIndex);
        return bucket.getEnvironment().computeInReadonlyTransaction(txn -> {
            ImmutableMap.Builder<String, TimestampObject<RetainedMessage>> builder = ImmutableMap.builder();
            Cursor cursor = bucket.getStore().openCursor(txn);
            Throwable throwable = null;
            try {
                if (cursor.getNext()) {
                    do {
                        String topic = serializer.deserializeTopic(XodusUtils.toBytes(cursor.getKey()));
                        if (filter.test(topic)) {
                            RetainedMessageEntity entity = serializer.deserializeEntry(XodusUtils.toBytes(cursor.getValue()));
                            if (entity.isDeleted()) {
                                builder.put(topic, new TimestampObject(null, entity.getTimestamp()));
                            } else {
                                builder.put(topic, new TimestampObject(entity, entity.getTimestamp()));
                            }
                        }
                    } while (cursor.getNext());
                }
            } catch (Throwable e) {
                throwable = e;
                throw e;
            } finally {
                XodusUtils.close(cursor, throwable);
            }
            return builder.build();
        });
    }

    public Set<String> getTopics(@NotNull Filter filter, int bucketIndex) {
        Preconditions.checkArgument(bucketIndex >= 0 && bucketIndex < this.bucketCount, "Bucket index out of range");
        Bucket bucket = this.buckets.get(bucketIndex);
        return bucket.getEnvironment().computeInReadonlyTransaction(txn -> {
            ImmutableSet.Builder<String> builder = ImmutableSet.builder();
            Cursor cursor = bucket.getStore().openCursor(txn);
            Throwable throwable = null;
            try {
                if (cursor.getNext()) {
                    do {
                        String topic = serializer.deserializeTopic(XodusUtils.toBytes(cursor.getKey()));
                        if (filter.test(topic)) {
                            RetainedMessageEntity entity = serializer.deserializeEntry(XodusUtils.toBytes(cursor.getValue()));
                            if (!entity.isDeleted()) {
                                builder.add(topic);
                            }
                        }
                    } while (cursor.getNext());
                }
            } catch (Throwable e) {
                throwable = e;
                throw e;
            } finally {
                XodusUtils.close(cursor, throwable);
            }
            return builder.build();
        });
    }

    public Set<String> removeAll(@NotNull Filter filter, int bucketIndex) {
        Preconditions.checkArgument(bucketIndex >= 0 && bucketIndex < this.bucketCount, "Bucket index out of range");
        Bucket bucket = this.buckets.get(bucketIndex);
        return bucket.getEnvironment().computeInTransaction(txn -> {
            ImmutableSet.Builder<String> builder = ImmutableSet.builder();
            Cursor cursor = bucket.getStore().openCursor(txn);
            Throwable throwable = null;
            try {
                if (cursor.getNext()) {
                    do {
                        String topic = serializer.deserializeTopic(XodusUtils.toBytes(cursor.getKey()));
                        if (filter.test(topic)) {
                            cursor.deleteCurrent();
                            builder.add(topic);
                        }
                    } while (cursor.getNext());
                }
            } catch (Throwable e) {
                throwable = e;
                throw e;
            } finally {
                XodusUtils.close(cursor, throwable);
            }
            return builder.build();
        });
    }

    public Set<String> cleanUp(long tombstoneMaxAge, int bucketIndex) {
        Preconditions.checkArgument(bucketIndex >= 0 && bucketIndex < this.bucketCount, "Bucket index out of range");
        Bucket bucket = this.buckets.get(bucketIndex);
        return bucket.getEnvironment().computeInTransaction(txn -> {
            ImmutableSet.Builder<String> builder = ImmutableSet.builder();
            Cursor cursor = bucket.getStore().openCursor(txn);
            Throwable throwable = null;
            try {
                if (cursor.getNext()) {
                    do {
                        String topic = serializer.deserializeTopic(XodusUtils.toBytes(cursor.getKey()));
                        RetainedMessageEntity entity = serializer.deserializeEntry(XodusUtils.toBytes(cursor.getValue()));
                        if (entity.isDeleted() && System.currentTimeMillis() - entity.getTimestamp() > tombstoneMaxAge) {
                            cursor.deleteCurrent();
                            builder.add(topic);
                        }
                    } while (cursor.getNext());
                }
            } catch (Throwable e) {
                throwable = e;
                throw e;
            } finally {
                XodusUtils.close(cursor, throwable);
            }
            return builder.build();
        });
    }

    public void close() {
        this.buckets.values().stream()
                .filter(bucket -> bucket.getEnvironment().isOpen())
                .forEach(bucket -> bucket.getEnvironment().close());
    }
}
