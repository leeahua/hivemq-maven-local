package bj1;

import av.PersistenceConfig;
import av.PersistenceConfigurationService;
import bc1.ClientSessionQueueEntry;
import bc1.QueuedMessagesLocalPersistence;
import bd1.PersistenceFolders;
import bg1.XodusUtils;
import bk1.ClearTransactionalExecutable;
import bk1.RemoveByClientExecutable;
import bk1.DeleteByFilterExecutable;
import bk1.OfferTransactionalExecutable;
import bk1.PeekTransactionalComputable;
import bk1.PollTransactionalComputable;
import bk1.GetEntriesTransactionalComputable;
import bk1.RemoveTransactionalComputable;
import bk1.OfferAllTransactionalComputable;
import bu.InternalPublish;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.hivemq.spi.annotations.NotNull;
import com.hivemq.spi.annotations.ThreadSafe;
import com.hivemq.spi.exceptions.UnrecoverableException;
import d.CacheScoped;
import jetbrains.exodus.ExodusException;
import jetbrains.exodus.env.Cursor;
import jetbrains.exodus.env.Environment;
import jetbrains.exodus.env.Environments;
import jetbrains.exodus.env.Store;
import jetbrains.exodus.env.StoreConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import u.Filter;
import u.TimestampObject;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.io.File;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@ThreadSafe
@CacheScoped
public class QueuedMessagesLocalXodusPersistence implements QueuedMessagesLocalPersistence {
    private static final Logger LOGGER = LoggerFactory.getLogger(QueuedMessagesLocalXodusPersistence.class);
    private static final String ENVIRONMENT_NAME = "queued_messages";
    public static final String CURRENT_VERSION = "030100";
    private QueuedMessagesSerializer serializer;
    private ConcurrentHashMap<String, Queue<Long>> lookupTable;
    private ConcurrentHashMap<String, Long> clientLastTimestamps;
    private Environment environment;
    private Store store;
    private final PersistenceFolders persistenceFolders;
    private final PersistenceConfigurationService persistenceConfigurationService;
    private boolean initialized = false;

    @Inject
    protected QueuedMessagesLocalXodusPersistence(
            PersistenceFolders persistenceFolders,
            PersistenceConfigurationService persistenceConfigurationService) {
        this.persistenceFolders = persistenceFolders;
        this.persistenceConfigurationService = persistenceConfigurationService;
    }

    @PostConstruct
    void init() {
        if (this.initialized) {
            return;
        }
        this.serializer = new QueuedMessagesSerializer();
        this.lookupTable = new ConcurrentHashMap<>();
        this.clientLastTimestamps = new ConcurrentHashMap<>();
        try {
            PersistenceConfig config = this.persistenceConfigurationService.getClientSessionQueuedMessagesConfig();
            this.environment = Environments.newInstance(
                    new File(this.persistenceFolders.create(ENVIRONMENT_NAME, CURRENT_VERSION), ENVIRONMENT_NAME),
                    XodusUtils.buildConfig(config, ENVIRONMENT_NAME));
            this.store = this.environment.computeInTransaction(txn ->
                    environment.openStore(ENVIRONMENT_NAME, StoreConfig.WITH_DUPLICATES, txn)
            );
        } catch (ExodusException e) {
            LOGGER.error("An error occurred while opening the Xodus Queued Messages Local persistence. Is another HiveMQ instance running?");
            LOGGER.debug("Original Exception:", e);
            throw new UnrecoverableException(false);
        }
        createLookupTable();
        this.initialized = true;
    }

    private void createLookupTable() {
        long startMillis = System.currentTimeMillis();
        LOGGER.debug("Creating Client Session Queued Messages Lookup Table");
        this.environment.executeInReadonlyTransaction(txn -> {
            Cursor cursor = store.openCursor(txn);
            while (cursor.getNext()) {
                String clientId = XodusUtils.toString(cursor.getKey());
                ClientSessionQueueEntry entry = serializer.deserialize(XodusUtils.toBytes(cursor.getValue()));
                putQueueIfAbsent(clientId);
                Queue<Long> queue = lookupTable.get(clientId);
                queue.offer(entry.getTimestamp());
            }
        });
        LOGGER.debug("Created Client Session Queued Messages Lookup Table in {}ms",
                System.currentTimeMillis() - startMillis);
    }

    public void clear() {
        LOGGER.trace("Clearing Client Session Queued Messages Lookup Table");
        long startMillis = System.currentTimeMillis();
        this.lookupTable.clear();
        this.environment.executeInTransaction(new ClearTransactionalExecutable(this.store));
        LOGGER.debug("Cleared Client Session Queued Messages Persistence in {}ms",
                System.currentTimeMillis() - startMillis);
    }

    public void close() {
        if (this.environment.isOpen()) {
            this.environment.close();
        }
    }

    public void offer(@NotNull String clientId, @NotNull InternalPublish publish, long timestamp) {
        Preconditions.checkNotNull(clientId, "Client must not be null");
        Preconditions.checkNotNull(publish, "Publish must not be null");
        Preconditions.checkArgument(timestamp > 0L, "Timestamp must be greater 0");
        putQueueIfAbsent(clientId);
        this.environment.executeInTransaction(
                new OfferTransactionalExecutable(clientId, publish,
                        this.lookupTable, this.store, this.serializer,
                        this.persistenceConfigurationService.getMaxQueuedMessages(),
                        this.persistenceConfigurationService.getQueuedMessagesStrategy()));
        this.clientLastTimestamps.put(clientId, timestamp);
    }

    public boolean queuePublishIfQueueNotEmpty(@NotNull String clientId, @NotNull InternalPublish publish) {
        Queue<Long> queue = this.lookupTable.get(clientId);
        if (queue == null || queue.size() == 0) {
            return false;
        }
        offer(clientId, publish, System.currentTimeMillis());
        return true;
    }

    private void putQueueIfAbsent(@NotNull String clientId) {
        Queue<Long> messagesQueue = this.lookupTable.get(clientId);
        if (messagesQueue == null) {
            LOGGER.trace("There was no Client Session Queued Messages Queue for client {}, creating the queue", clientId);
            messagesQueue = new PriorityQueue<>();
            this.lookupTable.put(clientId, messagesQueue);
        }
        this.clientLastTimestamps.put(clientId, System.currentTimeMillis());
    }

    @Nullable
    public ClientSessionQueueEntry poll(@NotNull String clientId, long timestamp) {
        Preconditions.checkNotNull(clientId, "Client must not be null");
        Queue<Long> messagesQueue = this.lookupTable.get(clientId);
        if (messagesQueue == null) {
            return null;
        }
        Long oldestTimestamp = messagesQueue.peek();
        if (oldestTimestamp == null) {
            LOGGER.error("An error in the persistence of Queued Messages occurred. Queue was null for client {} when polling", clientId);
            return null;
        }
        ClientSessionQueueEntry entry = this.environment.computeInTransaction(
                new PollTransactionalComputable(clientId, oldestTimestamp, this.lookupTable,
                        this.store, this.serializer));
        this.clientLastTimestamps.put(clientId, oldestTimestamp);
        return entry;
    }

    public ClientSessionQueueEntry peek(String clientId) {
        Preconditions.checkNotNull(clientId, "Client must not be null");
        Queue<Long> queue = this.lookupTable.get(clientId);
        if (queue == null) {
            return null;
        }
        Long oldestTimestamp = queue.peek();
        if (oldestTimestamp == null) {
            LOGGER.error("An error in the persistence of Queued Messages occurred. Queue was null for client {} when polling", clientId);
            return null;
        }
        return this.environment.computeInTransaction(
                new PeekTransactionalComputable(clientId, oldestTimestamp, this.lookupTable, this.store, this.serializer));
    }

    public void remove(@NotNull String clientId, long timestamp) {
        Preconditions.checkNotNull(clientId, "Client must not be null");
        Preconditions.checkArgument(timestamp > 0L, "Timestamp must be greater than 0");
        this.environment.executeInTransaction(new RemoveByClientExecutable(clientId, this.store));
        if (this.lookupTable.remove(clientId) != null) {
            this.clientLastTimestamps.put(clientId, timestamp);
            LOGGER.trace("Deleted Client Session Queued Messages Lookup table for client {}", clientId);
        }
    }

    public void remove(@NotNull String clientId, @NotNull String entryId, long entryTimestamp, long timestamp) {
        Preconditions.checkNotNull(clientId, "Client must not be null");
        Preconditions.checkNotNull(entryId, "Id must not be null");
        Preconditions.checkArgument(entryTimestamp > 0L, "Timestamp must be greater than 0");
        this.environment.executeInTransaction(
                new RemoveTransactionalComputable(clientId, entryTimestamp, entryId, this.lookupTable, this.store, this.serializer));
        this.clientLastTimestamps.put(clientId, timestamp);
    }

    public long size(@NotNull String clientId) {
        Preconditions.checkNotNull(clientId, "Client must not be null");
        Queue<Long> queue = this.lookupTable.get(clientId);
        if (queue == null) {
            return 0L;
        }
        return queue.size();
    }

    public ImmutableMap<String, TimestampObject<Set<ClientSessionQueueEntry>>> getEntries(Filter filter) {
        return this.environment.computeInReadonlyTransaction(
                new GetEntriesTransactionalComputable(filter, this.store, this.clientLastTimestamps, this.serializer));
    }

    public void offerAll(ImmutableSet<ClientSessionQueueEntry> queueEntries, String clientId, long timestamp) {
        putQueueIfAbsent(clientId);
        this.environment.executeInTransaction(
                new OfferAllTransactionalComputable(clientId, queueEntries,
                        this.lookupTable, this.store, this.serializer,
                        this.persistenceConfigurationService.getMaxQueuedMessages(),
                        this.persistenceConfigurationService.getQueuedMessagesStrategy()));
        this.clientLastTimestamps.put(clientId, timestamp);
    }

    public long getTimestamp(String clientId) {
        Preconditions.checkNotNull(clientId, "Client must not be null");
        return this.clientLastTimestamps.get(clientId);
    }

    public ImmutableSet<String> remove(Filter filter) {
        ImmutableSet.Builder<String> builder = ImmutableSet.builder();
        this.environment.executeInTransaction(new DeleteByFilterExecutable(filter, this.store));
        this.lookupTable.keySet().stream()
                .filter(filter::test)
                .forEach(this.lookupTable::remove);
        this.clientLastTimestamps.keySet().stream()
                .filter(filter::test)
                .forEach(this.clientLastTimestamps::remove);
        return builder.build();
    }

    public ImmutableSet<String> cleanUp(long tombstoneMaxAge) {
        ImmutableSet.Builder<String> builder = ImmutableSet.builder();
        this.clientLastTimestamps.keySet().stream()
                .filter(clientId -> {
                    ClientSessionQueueEntry entry = peek(clientId);
                    return entry == null &&
                            (System.currentTimeMillis() - this.clientLastTimestamps.get(clientId)) > tombstoneMaxAge;
                })
                .forEach(clientId -> {
                    this.environment.executeInTransaction(new RemoveByClientExecutable(clientId, this.store));
                    this.lookupTable.remove(clientId);
                    this.clientLastTimestamps.remove(clientId);
                    builder.add(clientId);
                });
        return builder.build();
    }
}
