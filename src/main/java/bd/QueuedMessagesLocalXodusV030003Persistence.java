package bd;

import av.PersistenceConfigurationService;
import av.PersistenceConfigurationService.QueuedMessagesStrategy;
import bd1.PersistenceFolders;
import bg1.XodusUtils;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.common.primitives.Bytes;
import com.hivemq.spi.annotations.NotNull;
import com.hivemq.spi.annotations.ReadOnly;
import com.hivemq.spi.annotations.ThreadSafe;
import com.hivemq.spi.exceptions.UnrecoverableException;
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
import jetbrains.exodus.env.Transaction;
import jetbrains.exodus.env.TransactionalComputable;
import jetbrains.exodus.env.TransactionalExecutable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@ThreadSafe
@CacheScoped
public class QueuedMessagesLocalXodusV030003Persistence {
    private static final Logger LOGGER = LoggerFactory.getLogger(QueuedMessagesLocalXodusV030003Persistence.class);
    private static final String ENVIRONMENT_NAME = "queued_messages";
    private final EntitySerializer serializer = new EntitySerializer();
    private Environment environment;
    private Store store;
    private final ConcurrentHashMap<String, Queue<Long>> lookupTable = new ConcurrentHashMap<>();
    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock(true);
    private final PersistenceFolders persistenceFolders;
    private final PersistenceConfigurationService persistenceConfigurationService;

    @Inject
    QueuedMessagesLocalXodusV030003Persistence(
            PersistenceFolders persistenceFolders,
            PersistenceConfigurationService persistenceConfigurationService) {
        this.persistenceFolders = persistenceFolders;
        this.persistenceConfigurationService = persistenceConfigurationService;
    }

    @PostConstruct
    public void a() {
        try {
            EnvironmentConfig config = getConfig(ENVIRONMENT_NAME, 60000);
            this.environment = Environments.newInstance(new File(this.persistenceFolders.root(), ENVIRONMENT_NAME), config);
            this.store = this.environment.computeInTransaction(txn ->
                    environment.openStore(ENVIRONMENT_NAME, StoreConfig.WITH_DUPLICATES, txn)
            );
        } catch (ExodusException e) {
            LOGGER.error("An error occurred while opening the Xodus Queued Messages Local persistence. Is another HiveMQ instance running?");
            LOGGER.debug("Original Exception:", e);
            throw new UnrecoverableException(false);
        }
        long startMillis = System.currentTimeMillis();
        LOGGER.debug("Creating Client Session Queued Messages Lookup Table");
        Lock lock = this.readWriteLock.writeLock();
        lock.lock();
        try {
            this.environment.executeInReadonlyTransaction(txn -> {
                Cursor cursor = store.openCursor(txn);
                while (cursor.getNext()) {
                    String clientId = XodusUtils.toString(cursor.getKey());
                    QueuedMessagesEntry entry = serializer.deserialize(XodusUtils.toBytes(cursor.getValue()));
                    putQueueIfAbsent(clientId);
                    Queue<Long> queue = lookupTable.get(clientId);
                    queue.offer(entry.getTimestamp());
                }
            });
        } finally {
            lock.unlock();
        }
        LOGGER.debug("Created Client Session Queued Messages Lookup Table in {}ms",
                System.currentTimeMillis() - startMillis);
    }

    public void migrate(Callback callback) {
        Lock lock = this.readWriteLock.readLock();
        lock.lock();
        try {
            this.environment.executeInReadonlyTransaction(txn -> {
                Cursor cursor = store.openCursor(txn);
                while (cursor.getNext()) {
                    String clientId = XodusUtils.toString(cursor.getKey());
                    QueuedMessagesEntry entry = serializer.deserialize(XodusUtils.toBytes(cursor.getValue()));
                    callback.persistent(clientId, entry);
                }
            });
        } finally {
            lock.unlock();
        }
    }

    @ReadOnly
    public Multimap<String, QueuedMessagesEntry> getEntries() {
        Lock lock = this.readWriteLock.readLock();
        lock.lock();
        try {
            return this.environment.computeInReadonlyTransaction(txn -> {
                Cursor cursor = store.openCursor(txn);
                ImmutableMultimap.Builder<String, QueuedMessagesEntry> builder = ImmutableMultimap.builder();
                while (cursor.getNext()) {
                    String clientId = XodusUtils.toString(cursor.getKey());
                    QueuedMessagesEntry entry = serializer.deserialize(XodusUtils.toBytes(cursor.getValue()));
                    builder.put(clientId, entry);
                }
                return builder.build();
            });
        } finally {
            lock.unlock();
        }
    }

    public void removeAll() {
        Lock lock = this.readWriteLock.writeLock();
        lock.lock();
        try {
            LOGGER.trace("Clearing Client Session Queued Messages Lookup Table");
            long startMillis = System.currentTimeMillis();
            this.lookupTable.clear();
            this.environment.executeInTransaction(txn -> {
                Cursor cursor = store.openCursor(txn);
                Throwable throwable = null;
                try {
                    while (cursor.getNext()) {
                        cursor.deleteCurrent();
                    }
                } catch (Throwable e) {
                    throwable = e;
                    throw e;
                } finally {
                    XodusUtils.close(cursor, throwable);
                }
            });
            LOGGER.debug("Cleared Client Session Queued Messages Persistence in {}ms", System.currentTimeMillis() - startMillis);
        } finally {
            lock.unlock();
        }
    }

    public void close() {
        Lock lock = this.readWriteLock.writeLock();
        lock.lock();
        try {
            if (this.environment.isOpen()) {
                this.environment.close();
            }
        } finally {
            lock.unlock();
        }
    }

    public void offer(@NotNull String clientId, @NotNull Publish publish, long timestamp, @NotNull String id) {
        Preconditions.checkNotNull(clientId, "Client must not be null");
        Preconditions.checkNotNull(id, "Id must not be null");
        Preconditions.checkArgument(timestamp > 0L, "Timestamp must be greater than 0");
        Preconditions.checkNotNull(publish, "Publish must not be null");
        Lock lock = this.readWriteLock.writeLock();
        lock.lock();
        try {
            putQueueIfAbsent(clientId);
            this.environment.executeInTransaction(new TransactionalExecutable() {

                @Override
                public void execute(@org.jetbrains.annotations.NotNull Transaction txn) {
                    ByteIterable searchKey = XodusUtils.toByteIterable(clientId);
                    Queue<Long> queue = lookupTable.get(clientId);
                    if (exists(txn, searchKey, queue)) {
                        LOGGER.debug("There is already a entry for client {}, timestamp {} and id {} in the Client Session Queue, ignoring",
                                clientId, timestamp, id);
                        return;
                    }
                    long maxQueuedMessages = persistenceConfigurationService.getMaxQueuedMessages();
                    while (queue.size() >= maxQueuedMessages) {
                        if (persistenceConfigurationService.getQueuedMessagesStrategy() == QueuedMessagesStrategy.DISCARD_OLDEST) {
                            deleteOldest(txn, queue.poll());
                            LOGGER.debug("Maximum Queued Message size of {} for client {} was already reached. Deleting oldest entry with timestamp {}",
                                    maxQueuedMessages, clientId, timestamp);
                        } else {
                            LOGGER.debug("Maximum Queued Message size of {} for client {} was already reached. Discarding message with timestamp {}",
                                    maxQueuedMessages, clientId, timestamp);
                            return;
                        }
                    }
                    QueuedMessagesEntry entry = new QueuedMessagesEntry(id, timestamp, publish.getTopic(), publish.getQoS(), publish.getPayload());
                    store.put(txn, searchKey, XodusUtils.toByteIterable(serializer.serialize(entry)));
                    queue.offer(timestamp);
                    LOGGER.trace("Added entry for client {} with timestamp {} and id {} to Client Session Queue",
                            clientId, timestamp, id);
                }

                private void deleteOldest(@NotNull Transaction txn, long timestamp) {
                    Cursor cursor = store.openCursor(txn);
                    Throwable throwable = null;
                    try {
                        ByteIterable searchBothRange = cursor.getSearchBothRange(XodusUtils.toByteIterable(clientId), XodusUtils.toByteIterable(serializer.serializeTimestamp(timestamp)));
                        do {
                            if (searchBothRange != null) {
                                ByteIterable clientIdByteIterable = XodusUtils.toByteIterable(clientId);
                                if (cursor.getKey().compareTo(clientIdByteIterable) == 0) {
                                    do {
                                        QueuedMessagesEntry entry = serializer.deserialize(XodusUtils.toBytes(cursor.getValue()));
                                        if (entry.getTimestamp() == timestamp) {
                                            cursor.deleteCurrent();
                                        }
                                    } while (cursor.getNextDup());
                                    return;
                                }
                            }
                        } while (cursor.getNext());
                    } catch (Throwable e) {
                        throwable = e;
                        throw e;
                    } finally {
                        XodusUtils.close(cursor, throwable);
                    }
                }

                private boolean exists(Transaction txn, ByteIterable searchKey, Queue<Long> queue) {
                    if (!queue.contains(timestamp)) {
                        return false;
                    }
                    byte[] existsKey = serializer.serialize(timestamp, id);
                    Cursor cursor = store.openCursor(txn);
                    Throwable throwable = null;
                    try {
                        ByteIterable existsKeyByteIterable = XodusUtils.toByteIterable(existsKey);
                        ByteIterable foundValues = cursor.getSearchKey(searchKey);
                        if (foundValues != null) {
                            do {
                                ByteIterable foundValue = cursor.getValue();
                                if (foundValue.subIterable(0, existsKeyByteIterable.getLength()).compareTo(existsKeyByteIterable) == 0) {
                                    return true;
                                }
                            } while (cursor.getNextDup());
                        }
                    } catch (Throwable e) {
                        throwable = e;
                        throw e;
                    } finally {
                        XodusUtils.close(cursor, throwable);
                    }
                    return false;
                }
            });
        } finally {
            lock.unlock();
        }
    }

    public boolean queuePublishIfQueueNotEmpty(@NotNull String clientId, @NotNull Publish publish, long timestamp, @NotNull String id) {
        Lock lock = this.readWriteLock.writeLock();
        lock.lock();
        try {
            Queue<Long> queue = this.lookupTable.get(clientId);
            if (queue == null || queue.size() == 0) {
                return false;
            }
            offer(clientId, publish, timestamp, id);
            return true;
        } finally {
            lock.unlock();
        }
    }

    private void putQueueIfAbsent(@NotNull String clientId) {
        Queue<Long> queue = this.lookupTable.get(clientId);
        if (queue == null) {
            LOGGER.trace("There was no Client Session Queued Messages Queue for client {}, creating the queue",
                    clientId);
            queue = new PriorityQueue<>();
            this.lookupTable.put(clientId, queue);
        }
    }

    @Nullable
    public QueuedMessagesEntry poll(@NotNull String clientId) {
        Preconditions.checkNotNull(clientId, "Client must not be null");
        Lock lock = this.readWriteLock.writeLock();
        lock.lock();
        try {
            Queue<Long> queue = this.lookupTable.get(clientId);
            if (queue != null) {
                Long timestamp = queue.peek();
                if (timestamp == null) {
                    LOGGER.error("An error in the persistence of Queued Messages occurred. Queue was null for client {} when polling", clientId);
                    return null;
                }
                return this.environment.computeInTransaction(new TransactionalComputable<QueuedMessagesEntry>() {

                    @Override
                    public QueuedMessagesEntry compute(@org.jetbrains.annotations.NotNull Transaction txn) {
                        ByteIterable searchKey = XodusUtils.toByteIterable(clientId);
                        Cursor cursor = store.openCursor(txn);
                        Throwable throwable = null;
                        try {
                            ByteIterable searchBothRange = cursor.getSearchBothRange(searchKey, XodusUtils.toByteIterable(serializer.serializeTimestamp(timestamp)));
                            if (searchBothRange != null) {
                                do {
                                    if (cursor.getKey().compareTo(searchKey) == 0) {
                                        QueuedMessagesEntry entry = serializer.deserialize(XodusUtils.toBytes(cursor.getValue()));
                                        removeLookupTable(clientId, timestamp);
                                        cursor.deleteCurrent();
                                        return entry;
                                    }
                                } while (cursor.getNext());
                            }
                            LOGGER.error("Potential inconsistency in the Client Session Queued Message Persistence: No entry for timestamp {} and client {} was found!",
                                    timestamp, clientId);
                            return null;
                        } catch (Throwable e) {
                            throwable = e;
                            throw e;
                        } finally {
                            XodusUtils.close(cursor, throwable);
                        }
                    }

                    protected void removeLookupTable(@NotNull String clientId, long timestamp) {
                        Queue<Long> queue = lookupTable.get(clientId);
                        if (queue == null) {
                            return;
                        }
                        if (queue.remove(timestamp)) {
                            LOGGER.trace("Deleted timestamp {} for client {} from ClientSession Queued Messages Lookup Table",
                                    timestamp, clientId);
                        } else {
                            LOGGER.trace("Could not delete entry with timestamp {} for client {} from ClientSession Queued Messages Lookup Table: Not found",
                                    timestamp, clientId);
                        }
                        if (queue.size() == 0) {
                            LOGGER.trace("Deleting Client Session Queued Messages Lookup table for client {} because the last queued message was deleted",
                                    clientId);
                            lookupTable.remove(clientId);
                        }
                    }
                });
            }
        } finally {
            lock.unlock();
        }
        return null;
    }

    public void remove(@NotNull String clientId, @NotNull String id, long timestamp) {
        Preconditions.checkNotNull(clientId, "Client must not be null");
        Preconditions.checkNotNull(id, "Id must not be null");
        Preconditions.checkArgument(timestamp > 0L, "Timestamp must be greater than 0");
        Lock lock = this.readWriteLock.writeLock();
        lock.lock();
        try {
            this.environment.executeInTransaction(new TransactionalExecutable(){

                @Override
                public void execute(@org.jetbrains.annotations.NotNull Transaction txn) {
                    Cursor cursor = store.openCursor(txn);
                    Throwable throwable = null;
                    try {
                        ByteIterable searchBothRange = cursor.getSearchBothRange(XodusUtils.toByteIterable(clientId), XodusUtils.toByteIterable(serializer.serializeTimestamp(timestamp)));
                        do {
                            if (searchBothRange != null) {
                                ByteIterable clientIdByteIterable = XodusUtils.toByteIterable(clientId);
                                if (cursor.getKey().compareTo(clientIdByteIterable) == 0) {
                                    do {
                                        removeCurrent(cursor);
                                    } while (cursor.getNextDup());
                                    return;
                                }
                            }
                        } while (cursor.getNext());
                    } catch (Throwable e) {
                        throwable = e;
                        throw e;
                    } finally {
                        XodusUtils.close(cursor, throwable);
                    }
                }

                private void removeCurrent(Cursor cursor) {
                    QueuedMessagesEntry entry = serializer.deserialize(XodusUtils.toBytes(cursor.getValue()));
                    if (entry.getTimestamp() == timestamp &&
                            entry.getId().equals(id)) {
                        cursor.deleteCurrent();
                        removeLookupTable(clientId, timestamp);
                    }
                }

                protected void removeLookupTable(@NotNull String clientId, long timestamp) {
                    Queue<Long> queue = lookupTable.get(clientId);
                    if (queue == null) {
                        return;
                    }
                    if (queue.remove(timestamp)) {
                        LOGGER.trace("Deleted timestamp {} for client {} from ClientSession Queued Messages Lookup Table",
                                timestamp, clientId);
                    } else {
                        LOGGER.trace("Could not delete entry with timestamp {} for client {} from ClientSession Queued Messages Lookup Table: Not found",
                                timestamp, clientId);
                    }
                    if (queue.size() == 0) {
                        LOGGER.trace("Deleting Client Session Queued Messages Lookup table for client {} because the last queued message was deleted",
                                clientId);
                        lookupTable.remove(clientId);
                    }
                }
            });
        } finally {
            lock.unlock();
        }
    }

    public void remove(@NotNull String clientId) {
        Preconditions.checkNotNull(clientId, "Client must not be null");
        Lock lock = this.readWriteLock.writeLock();
        lock.lock();
        try {
            this.environment.executeInTransaction(txn -> {
                if (store.delete(txn, XodusUtils.toByteIterable(clientId))) {
                    LOGGER.trace("Deleted all queued messages for client {}", clientId);
                } else {
                    LOGGER.trace("Tried to delete all queued messages for client {}", clientId);
                }
            });
            if (this.lookupTable.remove(clientId) != null) {
                LOGGER.trace("Deleted Client Session Queued Messages Lookup table for client {}", clientId);
            }
        } finally {
            lock.unlock();
        }
    }

    public long size(@NotNull String clientId) {
        Preconditions.checkNotNull(clientId, "Client must not be null");
        Lock lock = this.readWriteLock.readLock();
        lock.lock();
        try {
            Queue<Long> queue = this.lookupTable.get(clientId);
            if (queue == null) {
                return 0L;
            }
            return queue.size();
        } finally {
            lock.unlock();
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
        void persistent(String clientId, QueuedMessagesEntry entry);
    }

    public static class QueuedMessagesEntry {
        private final String id;
        private final long timestamp;
        private final String topic;
        private final QoS qoS;
        private final byte[] payload;

        public QueuedMessagesEntry(@NotNull String id,
                                   long timestamp,
                                   @NotNull String topic,
                                   @NotNull QoS qoS,
                                   @NotNull byte[] payload) {
            Preconditions.checkNotNull(id, "Unique ID must not be null");
            Preconditions.checkArgument(timestamp > 0L, "Timestamp must be an actual timestamp");
            Preconditions.checkNotNull(topic, "Topic must not be null");
            Preconditions.checkNotNull(qoS, "QoS must not be null");
            Preconditions.checkNotNull(payload, "Payload must not be null");
            this.id = id;
            this.timestamp = timestamp;
            this.topic = topic;
            this.qoS = qoS;
            this.payload = payload;
        }

        @NotNull
        public String getId() {
            return id;
        }

        public long getTimestamp() {
            return timestamp;
        }

        @NotNull
        public String getTopic() {
            return topic;
        }

        @NotNull
        public QoS getQoS() {
            return qoS;
        }

        @NotNull
        public byte[] getPayload() {
            return payload;
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if ((obj == null) || (getClass() != obj.getClass())) {
                return false;
            }
            QueuedMessagesEntry entry = (QueuedMessagesEntry) obj;
            return Objects.equals(this.timestamp, entry.timestamp) &&
                    Objects.equals(this.id, entry.id) &&
                    Objects.equals(this.topic, entry.topic) &&
                    Objects.equals(this.qoS, entry.qoS) &&
                    Objects.equals(this.payload, entry.payload);
        }

        public int hashCode() {
            return Objects.hash(this.id, this.timestamp, this.topic, this.qoS, this.payload);
        }
    }

    @ThreadSafe
    private static class EntitySerializer {
        public static final int TIMESTAMP_BYTE_LENGTH = 6;

        @NotNull
        public byte[] serialize(@NotNull QueuedMessagesEntry entry) {
            byte[] timestampBytes = serializeTimestamp(entry.getTimestamp());
            byte[] idBytes = entry.getId().getBytes(StandardCharsets.UTF_8);
            byte[] topicBytes = entry.getTopic().getBytes(StandardCharsets.UTF_8);
            byte qoSNumberByte = (byte) entry.getQoS().getQosNumber();
            byte[] payload = entry.getPayload();
            byte[] data = new byte[TIMESTAMP_BYTE_LENGTH + idBytes.length + 2 + topicBytes.length + 2 + 1 + payload.length];
            System.arraycopy(timestampBytes, 0, data, 0, TIMESTAMP_BYTE_LENGTH);
            data[6] = ((byte) (idBytes.length >> 8 & 0xFF));
            data[7] = ((byte) (idBytes.length & 0xFF));
            System.arraycopy(idBytes, 0, data, 8, idBytes.length);
            data[(8 + idBytes.length)] = ((byte) (topicBytes.length >> 8 & 0xFF));
            data[(9 + idBytes.length)] = ((byte) (topicBytes.length & 0xFF));
            System.arraycopy(topicBytes, 0, data, 10 + idBytes.length, topicBytes.length);
            data[(10 + idBytes.length + topicBytes.length)] = qoSNumberByte;
            System.arraycopy(payload, 0, data, 11 + idBytes.length + topicBytes.length, payload.length);
            return data;
        }

        @NotNull
        public QueuedMessagesEntry deserialize(@NotNull byte[] data) {
            long timestamp = deserializeTimestamp(data);
            int idLength = (data[TIMESTAMP_BYTE_LENGTH] & 0xFF) << 8 | data[7] & 0xFF;
            String id = new String(data, 8, idLength, StandardCharsets.UTF_8);
            int qoSNumber = (data[(8 + idLength)] & 0xFF) << 8 | data[(9 + idLength)] & 0xFF;
            String topic = new String(data, 10 + idLength, qoSNumber, StandardCharsets.UTF_8);
            QoS qoS = QoS.valueOf(data[(10 + idLength + qoSNumber)]);
            int payloadLength = data.length - 11 - idLength - qoSNumber;
            byte[] payload = new byte[payloadLength];
            System.arraycopy(data, 11 + idLength + qoSNumber, payload, 0, payloadLength);
            return new QueuedMessagesEntry(id, timestamp, topic, qoS, payload);
        }

        @NotNull
        public byte[] serialize(long timestamp, @NotNull String id) {
            byte[] clientIdBytes = id.getBytes(StandardCharsets.UTF_8);
            byte[] idLength = new byte[2];
            idLength[0] = ((byte) (clientIdBytes.length >> 8 & 0xFF));
            idLength[1] = ((byte) (clientIdBytes.length & 0xFF));
            return Bytes.concat(new byte[][]{
                    serializeTimestamp(timestamp),
                    idLength,
                    clientIdBytes});
        }

        public long deserializeTimestamp(@NotNull byte[] data) {
            return (data[5] & 0xFF) << 40 |
                    (data[4] & 0xFF) << 32 |
                    (data[3] & 0xFF) << 24 |
                    (data[2] & 0xFF) << 16 |
                    (data[1] & 0xFF) << 8 |
                    data[0] & 0xFF;
        }

        @NotNull
        public byte[] serializeTimestamp(long timestamp) {
            return new byte[]{
                    (byte) (int) timestamp,
                    (byte) (int) (timestamp >> 8),
                    (byte) (int) (timestamp >> 16),
                    (byte) (int) (timestamp >> 24),
                    (byte) (int) (timestamp >> 32),
                    (byte) (int) (timestamp >> 40)};
        }
    }
}
