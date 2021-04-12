package bg1;

import av.PersistenceConfigurationService;
import bc1.OutgoingMessageFlowLocalPersistence;
import bd1.PersistenceFolders;
import bh1.BucketUtils;
import bh1.LockBucket;
import bi.CachedMessages;
import bu.InternalPublish;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.hivemq.spi.annotations.NotNull;
import com.hivemq.spi.annotations.Nullable;
import com.hivemq.spi.annotations.ReadOnly;
import com.hivemq.spi.exceptions.UnrecoverableException;
import com.hivemq.spi.message.MessageWithId;
import com.hivemq.spi.message.PubRec;
import com.hivemq.spi.message.PubRel;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@CacheScoped
public class OutgoingMessageFlowLocalXodusPersistence implements OutgoingMessageFlowLocalPersistence {
    private static final Logger LOGGER = LoggerFactory.getLogger(OutgoingMessageFlowLocalXodusPersistence.class);
    public static final int BUCKET_COUNT = 64;
    private static final String ENVIRONMENT_NAME = "outgoing_message_flow";
    public static final String CURRENT_VERSION = "030100";
    private final PersistenceFolders persistenceFolders;
    private final PersistenceConfigurationService persistenceConfigurationService;
    private final CachedMessages cachedMessages = CachedMessages.INSTANCE;
    private ConcurrentHashMap<Integer, LockBucket> buckets;
    private OutgoingMessageFlowSerializer serializer;
    private boolean initialized = false;


    @Inject
    OutgoingMessageFlowLocalXodusPersistence(PersistenceFolders persistenceFolders,
                                             PersistenceConfigurationService persistenceConfigurationService) {
        this.persistenceFolders = persistenceFolders;
        this.persistenceConfigurationService = persistenceConfigurationService;
    }

    @PostConstruct
    void init() {
        if (this.initialized) {
            return;
        }
        try {
            this.serializer = new OutgoingMessageFlowSerializer();
            EnvironmentConfig config = XodusUtils.buildConfig(this.persistenceConfigurationService.getMessageFlowOutgoingConfig(), ENVIRONMENT_NAME);
            this.buckets = new ConcurrentHashMap<>();
            for (int bucket = 0; bucket < BUCKET_COUNT; bucket++) {
                Environment environment = Environments.newInstance(new File(this.persistenceFolders.create(ENVIRONMENT_NAME, "030100"), "outgoing_message_flow_" + bucket), config);
                Store store = environment.computeInTransaction(txn ->
                        environment.openStore(ENVIRONMENT_NAME, StoreConfig.WITHOUT_DUPLICATES, txn));
                this.buckets.put(bucket, new LockBucket(environment, store, new ReentrantReadWriteLock(true)));
            }
        } catch (ExodusException e) {
            LOGGER.error("An error occurred while opening the Xodus Outgoing Messages Local persistence. Is another HiveMQ instance running?");
            LOGGER.debug("Original Exception:", e);
            throw new UnrecoverableException();
        }
        this.initialized = true;
    }

    @ReadOnly
    public Set<String> getClients() {
        ImmutableSet.Builder<String> builder = ImmutableSet.builder();
        this.buckets.values().forEach(bucket -> {
            bucket.getLock().readLock().lock();
            try {
                builder.addAll(bucket.getEnvironment().computeInReadonlyTransaction(txn -> {
                    Cursor cursor = bucket.getStore().openCursor(txn);
                    Throwable throwable = null;
                    try {
                        ImmutableSet.Builder<String> bucketBuilder = ImmutableSet.builder();
                        while (cursor.getNext()) {
                            OutgoingMessageFlowKey key = serializer.deserializeKey(XodusUtils.toBytes(cursor.getKey()));
                            bucketBuilder.add(key.getClientId());
                        }
                        return bucketBuilder.build();
                    } catch (Throwable e) {
                        throwable = e;
                        throw e;
                    } finally {
                        XodusUtils.close(cursor, throwable);
                    }
                }));
            } finally {
                bucket.getLock().readLock().unlock();
            }
        });
        return builder.build();
    }

    public void close() {
        this.buckets.values().forEach(bucket -> {
            bucket.getLock().writeLock().lock();
            try {
                if (bucket.getEnvironment().isOpen()) {
                    bucket.getEnvironment().close();
                }
            } finally {
                bucket.getLock().writeLock().unlock();
            }
        });
    }

    @Nullable
    public MessageWithId get(@NotNull String clientId, int messageId) {
        Preconditions.checkNotNull(clientId, "Client must not be null");
        Preconditions.checkArgument(messageId >= 0 && messageId <= 65535, "Message ID must be between 0 and 65535 but was %s", messageId);
        LockBucket bucket = this.buckets.get(BucketUtils.bucket(clientId, BUCKET_COUNT));
        bucket.getLock().readLock().lock();
        try {
            return bucket.getEnvironment().computeInReadonlyTransaction(txn -> {
                ByteIterable foundValue = bucket.getStore().get(txn, XodusUtils.toByteIterable(serializer.serializeKey(clientId, messageId)));
                if (foundValue == null) {
                    return null;
                }
                MessageWithId message = serializer.deserializeMessage(XodusUtils.toBytes(foundValue));
                if (message instanceof PubRec) {
                    return cachedMessages.getPubRec(messageId);
                }
                if (message instanceof PubRel) {
                    return cachedMessages.getPubRel(messageId);
                }
                if (message instanceof InternalPublish) {
                    InternalPublish publish = (InternalPublish) message;
                    publish.setMessageId(messageId);
                    return publish;
                }
                return null;
            });
        } finally {
            bucket.getLock().readLock().unlock();
        }
    }

    public void addOrReplace(@NotNull String clientId, int messageId, @NotNull MessageWithId message) {
        Preconditions.checkNotNull(clientId, "Client must not be null");
        Preconditions.checkArgument(messageId >= 0 && messageId <= 65535, "Message ID must be between 0 and 65535 but was %s", messageId);
        Preconditions.checkNotNull(message, "Message must not be null");
        LockBucket bucket = this.buckets.get(BucketUtils.bucket(clientId, BUCKET_COUNT));
        bucket.getLock().writeLock().lock();
        try {
            bucket.getEnvironment().executeInTransaction(txn -> {
                ByteIterable key = XodusUtils.toByteIterable(serializer.serializeKey(clientId, messageId));
                ByteIterable value = XodusUtils.toByteIterable(serializer.serializeMessage(message));
                bucket.getStore().put(txn, key, value);
            });
        } finally {
            bucket.getLock().writeLock().unlock();
        }
    }

    @Nullable
    public MessageWithId remove(@NotNull String clientId, int messageId) {
        Preconditions.checkNotNull(clientId, "Client must not be null");
        Preconditions.checkArgument(messageId >= 0 && messageId <= 65535, "Message ID must be between 0 and 65535 but was %s", messageId);
        LockBucket bucket = this.buckets.get(BucketUtils.bucket(clientId, BUCKET_COUNT));
        bucket.getLock().writeLock().lock();
        try {
            return bucket.getEnvironment().computeInTransaction(txn -> {
                ByteIterable searchKey = XodusUtils.toByteIterable(serializer.serializeKey(clientId, messageId));
                ByteIterable foundValue = bucket.getStore().get(txn, searchKey);
                if (foundValue != null) {
                    MessageWithId message = serializer.deserializeMessage(XodusUtils.toBytes(foundValue));
                    if (message instanceof PubRel) {
                        bucket.getStore().delete(txn, searchKey);
                        return cachedMessages.getPubRel(messageId);
                    }
                    if (message instanceof InternalPublish) {
                        InternalPublish publish = (InternalPublish) message;
                        publish.setMessageId(messageId);
                        bucket.getStore().delete(txn, searchKey);
                        return publish;
                    }
                }
                return null;
            });
        } finally {
            bucket.getLock().writeLock().unlock();
        }
    }

    public void removeAll(@NotNull String clientId) {
        Preconditions.checkNotNull(clientId, "Client must not be null");
        LockBucket bucket = this.buckets.get(BucketUtils.bucket(clientId, BUCKET_COUNT));
        bucket.getLock().writeLock().lock();
        try {
            bucket.getEnvironment().executeInTransaction(txn -> {
                ByteIterable searchKey = XodusUtils.toByteIterable(clientId);
                Cursor cursor = bucket.getStore().openCursor(txn);
                Throwable throwable = null;
                try {
                    if (cursor.getSearchKeyRange(searchKey) == null) {
                        return;
                    }
                    do {
                        ByteIterable keyByteIterable = cursor.getKey();
                        OutgoingMessageFlowKey key = serializer.deserializeKey(XodusUtils.toBytes(keyByteIterable));
                        if (key.getClientId().equals(clientId)) {
                            LOGGER.trace("Deleting outgoing message entry with message id {} for client {}",
                                    key.getMessageId(), clientId);
                            cursor.deleteCurrent();
                        }
                    } while (cursor.getNext());
                } catch (Throwable e) {
                    throwable = e;
                    throw e;
                } finally {
                    XodusUtils.close(cursor, throwable);
                }
            });
        } finally {
            bucket.getLock().writeLock().unlock();
        }
    }

    public int size(@NotNull String clientId) {
        Preconditions.checkNotNull(clientId, "Client must not be null");
        LockBucket bucket = this.buckets.get(BucketUtils.bucket(clientId, BUCKET_COUNT));
        bucket.getLock().readLock().lock();
        try {
            return bucket.getEnvironment().computeInReadonlyTransaction(txn -> {
                int count = 0;
                ByteIterable searchKey = XodusUtils.toByteIterable(clientId);
                Cursor cursor = bucket.getStore().openCursor(txn);
                Throwable throwable = null;
                try {
                    if (cursor.getSearchKeyRange(searchKey) != null) {
                        do {
                            ByteIterable keyByteIterable = cursor.getKey();
                            OutgoingMessageFlowKey key = serializer.deserializeKey(XodusUtils.toBytes(keyByteIterable));
                            if (key.getClientId().equals(clientId)) {
                                count++;
                            }
                        } while (cursor.getNext());
                    }
                } catch (Throwable e) {
                    throwable = e;
                    throw e;
                } finally {
                    XodusUtils.close(cursor, throwable);
                }
                return count;
            });
        } finally {
            bucket.getLock().readLock().unlock();
        }
    }

    @ReadOnly
    public ImmutableList<MessageWithId> drain(@NotNull String clientId) {
        Preconditions.checkNotNull(clientId, "Client must not be null");
        LockBucket bucket = this.buckets.get(BucketUtils.bucket(clientId, BUCKET_COUNT));
        bucket.getLock().readLock().lock();
        try {
            return bucket.getEnvironment().computeInTransaction(txn -> {
                List<MessageWithId> messages = new ArrayList<>();
                ByteIterable searchKey = XodusUtils.toByteIterable(clientId);
                Cursor cursor = bucket.getStore().openCursor(txn);
                Throwable throwable = null;
                try {
                    if (cursor.getSearchKeyRange(searchKey) != null) {
                        do {
                            ByteIterable keyByteIterable = cursor.getKey();
                            OutgoingMessageFlowKey key = serializer.deserializeKey(XodusUtils.toBytes(keyByteIterable));
                            if (key.getClientId().equals(clientId)) {
                                MessageWithId message = serializer.deserializeMessage(XodusUtils.toBytes(cursor.getValue()));
                                if (message instanceof PubRec) {
                                    messages.add(cachedMessages.getPubRec(key.getMessageId()));
                                } else if (message instanceof PubRel) {
                                    messages.add(cachedMessages.getPubRel(key.getMessageId()));
                                } else if (message instanceof InternalPublish) {
                                    InternalPublish publish = (InternalPublish) message;
                                    publish.setMessageId(key.getMessageId());
                                    messages.add(publish);
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
                Collections.sort(messages, new MessageWithIdComparator());
                return ImmutableList.copyOf(messages);
            });
        } finally {
            bucket.getLock().readLock().unlock();
        }
    }

    public void putAll(@NotNull String clientId, @NotNull ImmutableList<MessageWithId> messages) {
        Preconditions.checkNotNull(clientId, "Client must not be null");
        Preconditions.checkNotNull(messages, "Messages must not be null");
        LockBucket bucket = this.buckets.get(BucketUtils.bucket(clientId, BUCKET_COUNT));
        bucket.getLock().writeLock().lock();
        try {
            bucket.getEnvironment().executeInTransaction(txn -> {
                ByteIterable searchKey = XodusUtils.toByteIterable(clientId);
                Cursor cursor = bucket.getStore().openCursor(txn);
                Throwable throwable = null;
                try {
                    if (cursor.getSearchKeyRange(searchKey) != null) {
                        do {
                            ByteIterable keyByteIterable = cursor.getKey();
                            OutgoingMessageFlowKey key = serializer.deserializeKey(XodusUtils.toBytes(keyByteIterable));
                            if (key.getClientId().equals(clientId)) {
                                cursor.deleteCurrent();
                            }
                        } while (cursor.getNext());
                    }
                } catch (Throwable e) {
                    throwable = e;
                    throw e;
                } finally {
                    XodusUtils.close(cursor, throwable);
                }
                messages.forEach(message -> {
                    ByteIterable key = XodusUtils.toByteIterable(serializer.serializeKey(clientId, message.getMessageId()));
                    ByteIterable value = XodusUtils.toByteIterable(serializer.serializeMessage(message));
                    bucket.getStore().put(txn, key, value);
                });
            });
        } finally {
            bucket.getLock().writeLock().unlock();
        }
    }

}
