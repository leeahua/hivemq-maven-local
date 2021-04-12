package bh;

import bd1.PersistenceFolders;
import bg1.XodusUtils;
import bh1.Bucket;
import bh1.BucketUtils;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Multimap;
import com.hivemq.spi.annotations.NotNull;
import com.hivemq.spi.annotations.ReadOnly;
import com.hivemq.spi.annotations.ThreadSafe;
import com.hivemq.spi.exceptions.UnrecoverableException;
import com.hivemq.spi.message.QoS;
import com.hivemq.spi.message.Topic;
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
import jetbrains.exodus.env.TransactionalExecutable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@CacheScoped
public class ClientSessionSubscriptionLocalXodusV030003Persistence {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientSessionSubscriptionLocalXodusV030003Persistence.class);
    public static final String ENVIRONMENT_NAME = "client_session_subscriptions";
    private static final int BUCKET_COUNT = 8;
    private ConcurrentHashMap<Integer, Bucket> buckets;
    final EntitySerializer serializer = new EntitySerializer();
    private final PersistenceFolders persistenceFolders;

    @Inject
    ClientSessionSubscriptionLocalXodusV030003Persistence(PersistenceFolders persistenceFolders) {
        this.persistenceFolders = persistenceFolders;
    }

    @PostConstruct
    public void init() {
        try {
            this.buckets = new ConcurrentHashMap<>();
            for (int bucket = 0; bucket < BUCKET_COUNT; bucket++) {
                EnvironmentConfig config = getConfig("client_session_subscriptions_" + bucket, 60000);
                Environment environment = Environments.newInstance(
                        new File(this.persistenceFolders.root(), "client_session_subscriptions_" + bucket), config);
                Store store = environment.computeInTransaction(txn ->
                        environment.openStore(ENVIRONMENT_NAME, StoreConfig.WITH_DUPLICATES, txn)
                );
                this.buckets.put(bucket, new Bucket(environment, store));
            }
        } catch (ExodusException e) {
            LOGGER.error("An error occurred while opening the Xodus Client Session Subscription Local persistence. Is another HiveMQ instance running?");
            LOGGER.debug("Original Exception:", e);
            throw new UnrecoverableException(false);
        }
    }

    public void migrate(Callback callback) {
        this.buckets.values().forEach(bucket ->
                bucket.getEnvironment().executeInReadonlyTransaction(txn -> {
                    Cursor cursor = bucket.getStore().openCursor(txn);
                    Throwable throwable = null;
                    try {
                        while (cursor.getNext()) {
                            String clientId = XodusUtils.toString(cursor.getKey());
                            Topic topic = serializer.deserialize(XodusUtils.toBytes(cursor.getValue()));
                            callback.persistent(clientId, topic);
                        }
                    } catch (Throwable e) {
                        throwable = e;
                        throw e;
                    } finally {
                        XodusUtils.close(cursor, throwable);
                    }
                })
        );
    }

    @ReadOnly
    public Multimap<String, Topic> allEntries() {
        ImmutableMultimap.Builder<String, Topic> builder = ImmutableMultimap.builder();
        this.buckets.values().forEach(bucket ->
                builder.putAll(bucket.getEnvironment().computeInReadonlyTransaction(txn -> {
                    ImmutableSetMultimap.Builder<String, Topic> bucketBuilder = ImmutableSetMultimap.builder();
                    Cursor cursor = bucket.getStore().openCursor(txn);
                    Throwable throwable = null;
                    try {
                        while (cursor.getNext()) {
                            String clientId = XodusUtils.toString(cursor.getKey());
                            Topic topic = this.serializer.deserialize(XodusUtils.toBytes(cursor.getValue()));
                            bucketBuilder.put(clientId, topic);
                        }
                    } catch (Throwable e) {
                        throwable = e;
                        throw e;
                    } finally {
                        XodusUtils.close(cursor, throwable);
                    }
                    return bucketBuilder.build();
                }))
        );
        return builder.build();
    }

    public void close() {
        this.buckets.values().stream()
                .filter(bucket -> bucket.getEnvironment().isOpen())
                .forEach(bucket -> bucket.getEnvironment().close());
    }

    public void addSubscription(@NotNull String clientId, @NotNull Topic topic) {
        LOGGER.trace("Adding subscription {} for client {} to persistent subscription store", topic.getTopic(), clientId);
        Preconditions.checkNotNull(clientId, "Client must not be null");
        Preconditions.checkNotNull(topic, "Topic must not be null");
        Preconditions.checkArgument(topic.getQoS() != null, "Topic QoS must not be null");
        Bucket bucket = this.buckets.get(BucketUtils.bucket(clientId, BUCKET_COUNT));
        bucket.getEnvironment().executeInTransaction(
                new AddSubscriptionTransactionalExecutable(topic, clientId, bucket.getStore(), this.serializer));
    }

    @ReadOnly
    public Set<Topic> getSubscriptions(@NotNull String clientId) {
        Preconditions.checkNotNull(clientId, "Client must not be null");
        LOGGER.trace("Returning subscriptions for client {}", clientId);
        Bucket bucket = this.buckets.get(BucketUtils.bucket(clientId, BUCKET_COUNT));
        return bucket.getEnvironment().computeInReadonlyTransaction(txn -> {
            ImmutableSet.Builder<Topic> builder = ImmutableSet.builder();
            Cursor cursor = bucket.getStore().openCursor(txn);
            Throwable throwable = null;
            try {
                ByteIterable foundValue = cursor.getSearchKey(XodusUtils.toByteIterable(clientId));
                if (foundValue != null) {
                    builder.add(serializer.deserialize(XodusUtils.toBytes(foundValue)));
                    while (cursor.getNextDup()) {
                        builder.add(serializer.deserialize(XodusUtils.toBytes(cursor.getValue())));
                    }
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

    public void removeAll(@NotNull String clientId) {
        Preconditions.checkNotNull("Client must not be null");
        LOGGER.trace("Deleting all subscriptions for client {}", clientId);
        Bucket bucket = this.buckets.get(BucketUtils.bucket(clientId, BUCKET_COUNT));
        bucket.getEnvironment().executeInTransaction(txn ->
                bucket.getStore().delete(txn, XodusUtils.toByteIterable(clientId))
        );
    }

    public void b(@NotNull String clientId, @NotNull Topic topic) {
        Preconditions.checkNotNull(clientId, "Client must not be null");
        Preconditions.checkNotNull(topic, "Topic must not be null");
        LOGGER.trace("Removing subscription '{}' for client '{}'", topic.getTopic(), clientId);
        Bucket bucket = this.buckets.get(BucketUtils.bucket(clientId, BUCKET_COUNT));
        bucket.getEnvironment().executeInTransaction(new RemoveTransactionalExecutable(topic, clientId, bucket.getStore(), this.serializer));
    }

    private EnvironmentConfig getConfig(@NotNull String name, int gcFilesDeletionDelay) {
        Preconditions.checkNotNull(name, "Name for environment config must not be null");
        EnvironmentConfig config = new EnvironmentConfig();
        config.setGcFilesDeletionDelay(gcFilesDeletionDelay);
        LOGGER.trace("Setting GC files deletion delay for persistence {} to {}ms", name, gcFilesDeletionDelay);
        return config;
    }

    public interface Callback {
        void persistent(String clientId, Topic topic);
    }

    @ThreadSafe
    private class EntitySerializer {
        static final int DEFAULT_QOS_NUMBER = -1;

        private EntitySerializer() {
        }

        @ThreadSafe
        public byte[] serialize(@NotNull Topic topic) {
            Preconditions.checkNotNull(topic, "Topic must not be null");
            int qoSNumber;
            if (topic.getQoS() != null) {
                qoSNumber = topic.getQoS().getQosNumber();
            } else {
                qoSNumber = DEFAULT_QOS_NUMBER;
            }
            byte[] topicBytes = topic.getTopic().getBytes(StandardCharsets.UTF_8);
            byte[] data = new byte[topicBytes.length + 1];
            data[0] = (byte) qoSNumber;
            System.arraycopy(topicBytes, 0, data, 1, topicBytes.length);
            return data;
        }

        @ThreadSafe
        public Topic deserialize(@NotNull byte[] data) {
            Preconditions.checkNotNull(data, "Bytes must not be null");
            Preconditions.checkArgument(data.length > 0, "Bytes must be greater than 1");
            int qoSNumber = data[0];
            String topic = new String(data, 1, data.length - 1, StandardCharsets.UTF_8);
            if (qoSNumber == -1) {
                return new Topic(topic, null);
            }
            return new Topic(topic, QoS.valueOf(qoSNumber));
        }
    }

    private static class RemoveTransactionalExecutable
            extends AbstractTransactionalExecutable {
        private final Topic topic;
        private final String clientId;
        private final EntitySerializer serializer;

        public RemoveTransactionalExecutable(@NotNull Topic topic,
                                             @NotNull String clientId,
                                             @NotNull Store paramStore,
                                             @NotNull EntitySerializer serializer) {
            super(topic, clientId, paramStore);
            this.topic = topic;
            this.clientId = clientId;
            this.serializer = serializer;
        }

        public void execute(Transaction txn) {
            delete(txn, XodusUtils.toByteIterable(this.serializer.serialize(this.topic)),
                    XodusUtils.toByteIterable(this.clientId));
        }
    }

    private static class AddSubscriptionTransactionalExecutable
            extends AbstractTransactionalExecutable {
        private final Topic topic;
        private final String clientId;
        private final Store store;
        private final EntitySerializer serializer;

        public AddSubscriptionTransactionalExecutable(@NotNull Topic topic,
                                                      @NotNull String clientId,
                                                      @NotNull Store store,
                                                      @NotNull EntitySerializer serializer) {
            super(topic, clientId, store);
            this.topic = topic;
            this.clientId = clientId;
            this.store = store;
            this.serializer = serializer;
        }

        public void execute(Transaction txn) {
            ByteIterable topicByteIterable = XodusUtils.toByteIterable(this.serializer.serialize(this.topic));
            ByteIterable clientIdByteIterable = XodusUtils.toByteIterable(this.clientId);
            delete(txn, topicByteIterable, clientIdByteIterable);
            this.store.put(txn, clientIdByteIterable, topicByteIterable);
        }
    }

    private static abstract class AbstractTransactionalExecutable
            implements TransactionalExecutable {
        private final Topic topic;
        private final String clientId;
        private final Store store;

        AbstractTransactionalExecutable(@NotNull Topic topic,
                                        @NotNull String clientId,
                                        @NotNull Store store) {
            this.topic = topic;
            this.clientId = clientId;
            this.store = store;
        }

        protected void delete(@NotNull Transaction transaction,
                              @NotNull ByteIterable topicByteIterable,
                              @NotNull ByteIterable clientByteIterable) {
            Cursor cursor = this.store.openCursor(transaction);
            Throwable throwable = null;
            try {
                cursor.getSearchKey(clientByteIterable);
                do {
                    if (cursor.getValue().getLength() == topicByteIterable.getLength()) {
                        delete(topicByteIterable, cursor);
                    }
                } while (cursor.getNextDup());
            } catch (Throwable e) {
                throwable = e;
                throw e;
            } finally {
                XodusUtils.close(cursor, throwable);
            }
        }

        private void delete(@NotNull ByteIterable topicByteIterable,
                            @NotNull Cursor cursor) {
            byte[] valueBytes = cursor.getValue().getBytesUnsafe();
            byte[] topicBytes = topicByteIterable.getBytesUnsafe();
            int offset = 1;
            int maxLength = cursor.getValue().getLength();
            for (int index = 1; index < maxLength; index++) {
                int valueByte = valueBytes[index];
                int topicByte = topicBytes[index];
                if (valueByte != topicByte) {
                    offset = 0;
                    break;
                }
            }
            if (offset != 0) {
                LOGGER.trace("Deleted old subscription for client {} and topic {}",
                        this.clientId, this.topic.getTopic());
                cursor.deleteCurrent();
            }
        }
    }
}
