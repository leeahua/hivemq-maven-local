package bb;

import bd1.PersistenceFolders;
import bg1.XodusUtils;
import bh1.BucketUtils;
import bh1.LockBucket;
import bi.CachedMessages;
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
import com.hivemq.spi.message.Publish;
import com.hivemq.spi.message.QoS;
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
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@CacheScoped
public class OutgoingMessageFlowLocalXodusV030003Persistence {
    private static final Logger LOGGER = LoggerFactory.getLogger(OutgoingMessageFlowLocalXodusV030003Persistence.class);
    private static final int BUCKET_COUNT = 64;
    private static final String ENVIRONMENT_NAME = "outgoing_message_flow";
    private final EntitySerializer serializer = new EntitySerializer();
    private final PersistenceFolders persistenceFolders;
    private final CachedMessages cachedMessages = CachedMessages.INSTANCE;
    private ConcurrentHashMap<Integer, LockBucket> buckets;

    @Inject
    OutgoingMessageFlowLocalXodusV030003Persistence(PersistenceFolders persistenceFolders) {
        this.persistenceFolders = persistenceFolders;
    }

    @PostConstruct
    public void init() {
        try {
            this.buckets = new ConcurrentHashMap<>();
            for (int bucket = 0; bucket < BUCKET_COUNT; bucket++) {
                EnvironmentConfig config = getConfig("outgoing_message_flow_" + bucket, 60000);
                Environment environment = Environments.newInstance(new File(this.persistenceFolders.root(),
                        "outgoing_message_flow_" + bucket), config);
                Store store = environment.computeInTransaction(txn ->
                        environment.openStore(ENVIRONMENT_NAME, StoreConfig.WITHOUT_DUPLICATES, txn)
                );
                this.buckets.put(bucket, new LockBucket(environment, store, new ReentrantReadWriteLock(true)));
            }
        } catch (ExodusException e) {
            LOGGER.error("An error occurred while opening the Xodus Outgoing Messages Local persistence. Is another HiveMQ instance running?");
            LOGGER.debug("Original Exception:", e);
            throw new UnrecoverableException();
        }
    }

    public void migrate(Callback callback) {
        this.buckets.values().forEach(bucket -> {
            bucket.getLock().readLock().lock();
            try {
                bucket.getEnvironment().executeInReadonlyTransaction(txn -> {
                    Cursor cursor = bucket.getStore().openCursor(txn);
                    Throwable throwable = null;
                    try {
                        while (cursor.getNext()) {
                            OutgoingMessageFlowKey key = serializer.deserializeKey(XodusUtils.toBytes(cursor.getKey()));
                            MessageWithId message = serializer.deserializeMessage(XodusUtils.toBytes(cursor.getValue()));
                            callback.persistent(key.getClientId(), key.getMessageId(), message);
                        }
                    } catch (Throwable e) {
                        throwable = e;
                        throw e;
                    } finally {
                        XodusUtils.close(cursor, throwable);
                    }
                });
            } finally {
                bucket.getLock().readLock().unlock();
            }
        });
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
        Preconditions.checkArgument((messageId >= 0) && (messageId <= 65535), "Message ID must be between 0 and 65535 but was %s",
                messageId);
        LockBucket bucket = this.buckets.get(BucketUtils.bucket(clientId, BUCKET_COUNT));
        bucket.getLock().readLock().lock();
        try {
            return bucket.getEnvironment().computeInReadonlyTransaction(txn -> {
                ByteIterable foundValue = bucket.getStore().get(txn, XodusUtils.toByteIterable(serializer.serializeKey(clientId, messageId)));
                if (foundValue != null) {
                    MessageWithId message = serializer.deserializeMessage(XodusUtils.toBytes(foundValue));
                    if (message instanceof PubRec) {
                        return cachedMessages.getPubRec(messageId);
                    }
                    if (message instanceof PubRel) {
                        return cachedMessages.getPubRel(messageId);
                    }
                    if (message instanceof Publish) {
                        Publish publish = (Publish) message;
                        publish.setMessageId(messageId);
                        return publish;
                    }
                }
                return null;
            });
        } finally {
            bucket.getLock().readLock().unlock();
        }
    }

    public void addOrReplace(@NotNull String clientId, int messageId, @NotNull MessageWithId message) {
        Preconditions.checkNotNull(clientId, "Client must not be null");
        Preconditions.checkArgument((messageId >= 0) && (messageId <= 65535), "Message ID must be between 0 and 65535 but was %s", messageId);
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
        Preconditions.checkArgument((messageId >= 0) && (messageId <= 65535), "Message ID must be between 0 and 65535 but was %s", messageId);
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
                    if (message instanceof Publish) {
                        Publish localPublish = (Publish) message;
                        localPublish.setMessageId(messageId);
                        bucket.getStore().delete(txn, searchKey);
                        return localPublish;
                    }
                }
                return null;
            });
        } finally {
            bucket.getLock().writeLock().unlock();
        }
    }

    public void remove(@NotNull String clientId) {
        Preconditions.checkNotNull(clientId, "Client must not be null");
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
                                LOGGER.trace("Deleting outgoing message entry with message id {} for client {}",
                                        key.getMessageId(), clientId);
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
    public List<MessageWithId> drain(@NotNull String clientId) {
        Preconditions.checkNotNull(clientId, "Client must not be null");
        LockBucket bucket = this.buckets.get(BucketUtils.bucket(clientId, BUCKET_COUNT));
        bucket.getLock().readLock().lock();
        try {
            return bucket.getEnvironment().computeInTransaction(txn -> {
                ImmutableList.Builder<MessageWithId> messages = ImmutableList.builder();
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
                                } else if (message instanceof Publish) {
                                    Publish publish = (Publish) message;
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
                return messages.build();
            });
        } finally {
            bucket.getLock().readLock().unlock();
        }
    }

    private EnvironmentConfig getConfig(@NotNull String name, int gcFilesDeletionDelay) {
        Preconditions.checkNotNull(name, "Name for environment config must not be null");
        EnvironmentConfig config = new EnvironmentConfig();
        config.setGcFilesDeletionDelay(gcFilesDeletionDelay);
        LOGGER.trace("Setting GC files deletion delay for persistence {} to {}ms",
                name, gcFilesDeletionDelay);
        return config;
    }

    public interface Callback {
        void persistent(String clientId, int messageId, MessageWithId message);
    }

    private static class EntitySerializer {
        private static final Logger LOGGER = LoggerFactory.getLogger(EntitySerializer.class);
        private static final byte[] PUB_REC_BYTES = {0};
        private static final byte[] PUB_REL_BYTES = {-64};
        private final CachedMessages cachedMessages = CachedMessages.INSTANCE;

        @NotNull
        public byte[] serializeKey(@NotNull String clientId, int messageId) {
            Preconditions.checkNotNull(clientId, "Client must not be null");
            Preconditions.checkArgument((messageId >= 0) && (messageId <= 65535), "Message id must be between 0 and 65535. Was %s", messageId);
            byte[] clientIdBytes = clientId.getBytes(StandardCharsets.UTF_8);
            byte[] messageIdBytes = serializeMessageId(messageId);
            byte[] data = new byte[clientIdBytes.length + messageIdBytes.length];
            System.arraycopy(clientIdBytes, 0, data, 0, clientIdBytes.length);
            System.arraycopy(messageIdBytes, 0, data, clientIdBytes.length, messageIdBytes.length);
            return data;
        }

        @NotNull
        public OutgoingMessageFlowKey deserializeKey(@NotNull byte[] data) {
            Preconditions.checkNotNull(data, "Byte array must not be null");
            int messageId = deserializeMessageId(data, data.length - 2);
            String clientId = new String(data, 0, data.length - 2, StandardCharsets.UTF_8);
            return new OutgoingMessageFlowKey(clientId, messageId);
        }

        @NotNull
        public byte[] serializeMessage(@NotNull MessageWithId message) {
            Preconditions.checkArgument(message instanceof Publish || message instanceof PubRec || message instanceof PubRel, "Message must be a Publish or a PubRec or a PubRel");
            if (message instanceof PubRec) {
                return PUB_REC_BYTES;
            }
            if (message instanceof PubRel) {
                return PUB_REL_BYTES;
            }
            return serializePublish((Publish) message);
        }

        @NotNull
        public MessageWithId deserializeMessage(@NotNull byte[] data) {
            Preconditions.checkNotNull(data, "Byte array must not be null");
            if (data[0] == PUB_REC_BYTES[0]) {
                return this.cachedMessages.getPubRec(0);
            }
            if (data[0] == PUB_REL_BYTES[0]) {
                return this.cachedMessages.getPubRel(0);
            }
            if ((data[0] & 0x80) == 128) {
                return deserializePublish(data);
            }
            LOGGER.error("Could not deserialize!");
            throw new IllegalArgumentException("Invalid value to deserialize!");
        }

        private byte[] serializePublish(@NotNull Publish publish) {
            int flag = -128;
            flag = (byte) (flag | publish.getQoS().getQosNumber());
            if (publish.isDuplicateDelivery()) {
                flag = (byte) (flag | 0x8);
            }
            if (publish.isRetain()) {
                flag = (byte) (flag | 0x4);
            }
            byte[] topicBytes = publish.getTopic().getBytes(StandardCharsets.UTF_8);
            byte[] payload = publish.getPayload();
            byte[] data = new byte[1 + topicBytes.length + 2 + payload.length];
            data[0] = (byte) flag;
            data[1] = ((byte) (topicBytes.length >> 8 & 0xFF));
            data[2] = ((byte) (topicBytes.length & 0xFF));
            System.arraycopy(topicBytes, 0, data, 3, topicBytes.length);
            System.arraycopy(payload, 0, data, 3 + topicBytes.length, payload.length);
            return data;
        }

        @NotNull
        private Publish deserializePublish(@NotNull byte[] data) {
            Publish publish = new Publish();
            publish.setQoS(QoS.valueOf(data[0] & 0x3));
            publish.setDuplicateDelivery((data[0] & 0x8) == 8);
            publish.setRetain((data[0] & 0x4) == 4);
            int topicLength = data[1] << 8 & 0xFF00 | data[2] & 0xFF;
            publish.setTopic(new String(data, 3, topicLength, StandardCharsets.UTF_8));
            int payloadLength = data.length - 3 - topicLength;
            byte[] payload = new byte[payloadLength];
            System.arraycopy(data, 3 + topicLength, payload, 0, payloadLength);
            publish.setPayload(payload);
            return publish;
        }

        private int deserializeMessageId(@NotNull byte[] data, int offset) {
            return (data[(offset + 1)] & 0xFF) << 8 | data[offset] & 0xFF;
        }

        @NotNull
        private byte[] serializeMessageId(int messageId) {
            return new byte[]{(byte) messageId, (byte) (messageId >> 8)};
        }
    }

    public static class OutgoingMessageFlowKey {
        private final String clientId;
        private final int messageId;

        public OutgoingMessageFlowKey(@NotNull String clientId, int messageId) {
            this.clientId = clientId;
            this.messageId = messageId;
        }

        public String getClientId() {
            return clientId;
        }

        public int getMessageId() {
            return messageId;
        }
    }
}
