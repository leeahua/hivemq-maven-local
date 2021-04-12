package bi1;

import av.PersistenceConfigurationService;
import bc1.ClientSessionSubscriptionsLocalPersistence;
import bd1.PersistenceFolders;
import bg1.XodusUtils;
import bh1.Bucket;
import bh1.BucketUtils;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.hivemq.spi.annotations.NotNull;
import com.hivemq.spi.exceptions.UnrecoverableException;
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
import org.eclipse.jetty.util.ConcurrentHashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import u.Filter;
import u.TimestampObject;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

// TODO:
@CacheScoped
public class ClientSessionSubscriptionsLocalXodusPersistence
        implements ClientSessionSubscriptionsLocalPersistence {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientSessionSubscriptionsLocalXodusPersistence.class);
    public static final String NAME = "client_session_subscriptions";
    private static final int BUCKET_COUNT = 8;
    public static final String CURRENT_VERSION = "030100";
    private AtomicLong currentVector;
    private final Map<String, Long> lastTimestamps = new ConcurrentHashMap<>();
    private ConcurrentHashMap<Integer, Bucket> buckets;
    private ClientSessionSubscriptionSerializer serializer;
    private final PersistenceFolders persistenceFolders;
    private final PersistenceConfigurationService persistenceConfigurationService;
    private boolean initialized = false;

    @Inject
    protected ClientSessionSubscriptionsLocalXodusPersistence(
            PersistenceFolders persistenceFolders,
            PersistenceConfigurationService persistenceConfigurationService) {
        this.persistenceFolders = persistenceFolders;
        this.persistenceConfigurationService = persistenceConfigurationService;
    }

    @PostConstruct
    protected void init() {
        if (this.initialized) {
            return;
        }
        this.serializer = new ClientSessionSubscriptionSerializer();
        try {
            EnvironmentConfig config = XodusUtils.buildConfig(
                    this.persistenceConfigurationService.getClientSessionSubscriptionConfig(),
                    NAME);
            this.buckets = new ConcurrentHashMap<>();
            for (int bucket = 0; bucket < BUCKET_COUNT; bucket++) {
                Environment environment = Environments.newInstance(
                        new File(this.persistenceFolders.create(NAME, CURRENT_VERSION),
                                "client_session_subscriptions_" + bucket),
                        config);
                Store store = environment.computeInTransaction(txn ->
                        environment.openStore(NAME, StoreConfig.WITH_DUPLICATES_WITH_PREFIXING, txn));
                this.buckets.put(bucket, new Bucket(environment, store));
            }
        } catch (ExodusException e) {
            LOGGER.error("An error occurred while opening the Xodus Client Session Subscription Local persistence. Is another HiveMQ instance running?");
            LOGGER.debug("Original Exception:", e);
            throw new UnrecoverableException(false);
        }
        prepare();
        this.initialized = true;
    }

    private void prepare() {
        try {
            long[] vectorArray = {0L};
            this.buckets.values().forEach(bucket ->
                    bucket.getEnvironment().executeInReadonlyTransaction(txn -> {
                        Cursor cursor = bucket.getStore().openCursor(txn);
                        Throwable throwable = null;
                        try {
                            while (cursor.getNext()) {
                                ClientSessionSubscriptionKey key = serializer.deserializeKey(XodusUtils.toBytes(cursor.getKey()));
                                long localVector = serializer.deserializeVector(XodusUtils.toBytes(cursor.getValue()));
                                Long vector = this.lastTimestamps.get(key.getClientId());
                                if (vector == null || vector < localVector) {
                                    this.lastTimestamps.put(key.getClientId(), localVector);
                                }
                                if (key.getTimestamp() > vectorArray[0]) {
                                    vectorArray[0] = key.getTimestamp();
                                }
                            }
                        } catch (Throwable e) {
                            throwable = e;
                            throw e;
                        } finally {
                            XodusUtils.close(cursor, throwable);
                        }
                    })
            );
            this.currentVector = new AtomicLong(vectorArray[0]);
        } catch (ExodusException e) {
            LOGGER.error("An error occurred while preparing the Client Session Subscription persistence.");
            LOGGER.debug("Original Exception:", e);
            throw new UnrecoverableException(false);
        }
    }

    public void addSubscription(@NotNull String clientId, @NotNull Topic topic, long timestamp) {
        Preconditions.checkNotNull(clientId, "Clientid must not be null");
        Preconditions.checkNotNull(topic, "Topic must not be null");
        Preconditions.checkNotNull(topic.getTopic(), "Topic must not be null");
        Preconditions.checkState(timestamp > 0L, "Timestamp must not be 0");
        Bucket bucket = this.buckets.get(BucketUtils.bucket(clientId, BUCKET_COUNT));
        bucket.getEnvironment().executeInTransaction(txn -> {
            long vector = currentVector.getAndIncrement();
            ByteIterable searchKey = XodusUtils.toByteIterable(serializer.serializeKey(clientId, vector));
            bucket.getStore().put(txn, searchKey, XodusUtils.toByteIterable(serializer.serialize(topic, timestamp, vector)));
            lastTimestamps.put(clientId, timestamp);
        });
    }

    public ImmutableSet<Topic> getSubscriptions(@NotNull String clientId) {
        Preconditions.checkNotNull(clientId, "client id must not be null");
        Bucket bucket = this.buckets.get(BucketUtils.bucket(clientId, BUCKET_COUNT));
        return bucket.getEnvironment().computeInReadonlyTransaction(txn -> {
            Map<Topic, Long> topics = new HashMap<>();
            Cursor cursor = bucket.getStore().openCursor(txn);
            Throwable throwable = null;
            try {
                ByteIterable foundValues = cursor.getSearchKeyRange(XodusUtils.toByteIterable(serializer.serializeClientId(clientId)));
                if (foundValues == null) {
                    return ImmutableSet.of();
                }
                do {
                    byte[] valueBytes = XodusUtils.toBytes(cursor.getValue());
                    Topic topic = serializer.deserialize(valueBytes);
                    long timestamp = serializer.deserializeTimestamp(valueBytes);
                    Long subscriptionTimestamp = topics.get(topic);
                    if (subscriptionTimestamp == null) {
                        topics.put(topic, timestamp);
                    } else if (subscriptionTimestamp < timestamp) {
                        topics.put(topic, timestamp);
                    }
                } while (cursor.getNext());
            } catch (Throwable t) {
                throwable = t;
                throw t;
            } finally {
                XodusUtils.close(cursor, throwable);
            }
            return ImmutableSet.copyOf(topics.keySet());
        });
    }

    public void removeAllSubscriptions(@NotNull String clientId, long timestamp) {
        Preconditions.checkNotNull(clientId, "Client id must not be null");
        Preconditions.checkState(timestamp > 0L, "Timestamp must not be 0");
        Bucket bucket = this.buckets.get(BucketUtils.bucket(clientId, BUCKET_COUNT));
        bucket.getEnvironment().executeInTransaction(txn -> {
            Cursor cursor = bucket.getStore().openCursor(txn);
            Throwable throwable = null;
            try {
                ByteIterable foundValues = cursor.getSearchKeyRange(
                        XodusUtils.toByteIterable(serializer.serializeClientId(clientId)));
                if (foundValues == null) {
                    return;
                }
                do {
                    cursor.deleteCurrent();
                } while (cursor.getNext());
            } catch (Throwable e) {
                throwable = e;
                throw e;
            } finally {
                XodusUtils.close(cursor, throwable);
            }
            lastTimestamps.put(clientId, timestamp);
        });
    }

    public void removeSubscription(@NotNull String clientId, @NotNull Topic topic, long timestamp) {
        Preconditions.checkNotNull(clientId, "client id must not be null");
        Preconditions.checkNotNull(topic, "Topic must not be null");
        Preconditions.checkState(timestamp > 0L, "Timestamp must not be 0");
        Bucket bucket = this.buckets.get(BucketUtils.bucket(clientId, BUCKET_COUNT));
        bucket.getEnvironment().executeInTransaction(txn -> {
            Cursor cursor = bucket.getStore().openCursor(txn);
            Throwable throwable = null;
            try {
                ByteIterable foundValues = cursor.getSearchKeyRange(
                        XodusUtils.toByteIterable(serializer.serializeClientId(clientId)));
                if (foundValues == null) {
                    return;
                }
                do {
                    ByteIterable foundValue = cursor.getValue();
                    Topic foundTopic = serializer.deserialize(foundValue);
                    if (foundTopic.getTopic().equals(topic.getTopic())) {
                        cursor.deleteCurrent();
                        lastTimestamps.put(clientId, timestamp);
                    }
                } while (cursor.getNext());
            } catch (Throwable e) {
                throwable = e;
                throw e;
            } finally {
                XodusUtils.close(cursor, throwable);
            }
        });
    }

    public Long getTimestamp(@NotNull String clientId) {
        Preconditions.checkNotNull(clientId, "Clientid must not be null");
        return this.lastTimestamps.get(clientId);
    }

    public Map<String, TimestampObject<Set<Topic>>> getEntries(@NotNull Filter filter) {
        Preconditions.checkNotNull(filter, "Filter must not be null");
        Map<String, TimestampObject<Set<Topic>>> localConcurrentHashMap = new ConcurrentHashMap();
        this.buckets.values().forEach(bucket -> bucket.getEnvironment().executeInReadonlyTransaction(txn -> {
            Cursor cursor = bucket.getStore().openCursor(txn);
            HashMap<String, Map<String, Long>> localHashMap = new HashMap();
            while (cursor.getNext()) {
                ClientSessionSubscriptionKey key = serializer.deserializeKey(XodusUtils.toBytes(cursor.getKey()));
                String clientId = key.getClientId();
                if (!filter.test(clientId)) {
                    continue;
                }
                Topic topic = serializer.deserialize(cursor.getValue());
                Map<String, Long> subscriptions = localHashMap.get(clientId);
                if (subscriptions == null) {
                    subscriptions = new HashMap<>();
                    localHashMap.put(clientId, subscriptions);
                    subscriptions.put(topic.getTopic(), key.getTimestamp());
                } else {
                    Long localObject2 = subscriptions.get(topic.getTopic());
                    if (localObject2 != null && localObject2 >= key.getTimestamp()) {
                        continue;
                    }
                }
                TimestampObject<Set<Topic>> timestampObject = localConcurrentHashMap.get(clientId);
                if (timestampObject == null) {
                    timestampObject = new TimestampObject(new HashSet<Topic>(), lastTimestamps.get(clientId));
                    localConcurrentHashMap.put(clientId, timestampObject);
                }
                timestampObject.getObject().add(topic);
            }
        }));
        this.lastTimestamps.forEach((clientId, timestamp) -> {
            if (filter.test(clientId) &&
                    !localConcurrentHashMap.containsKey(clientId)) {
                localConcurrentHashMap.put(clientId, new TimestampObject(Collections.emptySet(), timestamp));
            }
        });
        return localConcurrentHashMap;
    }

    public Set<String> remove(Filter filter) {
        Preconditions.checkNotNull(filter, "Filter must not be null");
        Set<String> deletedClients = new ConcurrentHashSet<>();
        this.buckets.values().forEach(bucket -> bucket.getEnvironment().executeInTransaction(txn -> {
            Cursor cursor = bucket.getStore().openCursor(txn);
            while (cursor.getNext()) {
                String clientId = serializer.deserializeKey(XodusUtils.toBytes(cursor.getKey())).getClientId();
                if (filter.test(clientId)) {
                    cursor.deleteCurrent();
                    deletedClients.add(clientId);
                    lastTimestamps.remove(clientId);
                }
            }
        }));
        return deletedClients;
    }

    private Set<String> d() {
        Set<String> localConcurrentHashSet = new ConcurrentHashSet();
        this.buckets.values().forEach(bucket -> bucket.getEnvironment().executeInTransaction(txn -> {
            Cursor cursor = bucket.getStore().openCursor(txn);
            while (cursor.getNext()) {
                String clientId = serializer.deserializeKey(XodusUtils.toBytes(cursor.getKey())).getClientId();
                localConcurrentHashSet.add(clientId);
            }
        }));
        return localConcurrentHashSet;
    }

    public Set<String> cleanUp(long tombstoneMaxAge) {
        Set<String> localSet = b(tombstoneMaxAge);
        e();
        return localSet;
    }

    private void e() {
        this.lastTimestamps.keySet().forEach(clientId -> {
            Bucket bucket = this.buckets.get(BucketUtils.bucket(clientId, BUCKET_COUNT));
            Map<String, SortedSet<Long>> localHashMap = new HashMap();
            bucket.getEnvironment().executeInReadonlyTransaction(txn -> {
                Cursor cursor = bucket.getStore().openCursor(txn);
                Throwable throwable = null;
                try {
                    cursor.getSearchKeyRange(XodusUtils.toByteIterable(serializer.serializeClientId(clientId)));
                    if (cursor.getKey().getLength() < 1) {
                        return;
                    }
                    do {
                        Long timestamp = serializer.deserializeTimestamp(XodusUtils.toBytes(cursor.getKey()));
                        Topic topic = serializer.deserialize(cursor.getValue());
                        SortedSet localObject2 = localHashMap.get(topic.getTopic());
                        if (localObject2 == null) {
                            localObject2 = new TreeSet();
                            localHashMap.put(topic.getTopic(), localObject2);
                        }
                        localObject2.add(timestamp);
                    } while (cursor.getNextDup());
                } catch (Throwable e) {
                    throwable = e;
                    throw e;
                } finally {
                    XodusUtils.close(cursor, throwable);
                }
            });
            localHashMap.values().stream()
                    .filter(set -> set.size() >= 2)
                    .forEach(set ->
                            bucket.getEnvironment().executeInTransaction(txn ->
                                    set.stream()
                                            .filter(timestamp -> timestamp < set.last())
                                            .forEach(timestamp -> bucket.getStore().delete(txn,
                                                    XodusUtils.toByteIterable(serializer.serializeKey(clientId, timestamp))))
                            ));
        });
    }

    @NotNull
    private Set<String> b(long tombstoneMaxAge) {
        Set<String> localHashSet = new HashSet<>();
        Set<String> localSet = d();
        long timestamp = System.currentTimeMillis();
        this.lastTimestamps.entrySet().stream()
                .filter(entry -> !localSet.contains(entry.getKey()) &&
                        timestamp - entry.getValue() > tombstoneMaxAge)
                .forEach(entry -> {
                    localHashSet.add(entry.getKey());
                    this.lastTimestamps.remove(entry.getKey());
                });
        return localHashSet;
    }

    public void close() {
        this.buckets.values().stream()
                .filter(bucket -> bucket.getEnvironment().isOpen())
                .forEach(bucket -> bucket.getEnvironment().close());
    }
}
