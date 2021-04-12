package bf;

import bd1.PersistenceFolders;
import bz.RetainedMessage;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.ByteStreams;
import com.hivemq.spi.annotations.NotNull;
import com.hivemq.spi.annotations.Nullable;
import com.hivemq.spi.exceptions.UnrecoverableException;
import com.hivemq.spi.message.QoS;
import d.CacheScoped;
import jetbrains.exodus.ExodusException;
import jetbrains.exodus.entitystore.Entity;
import jetbrains.exodus.entitystore.EntityIterable;
import jetbrains.exodus.entitystore.PersistentEntityStoreImpl;
import jetbrains.exodus.entitystore.PersistentEntityStores;
import jetbrains.exodus.entitystore.StoreTransaction;
import jetbrains.exodus.entitystore.StoreTransactionalExecutable;
import jetbrains.exodus.env.EnvironmentConfig;
import jetbrains.exodus.env.Environments;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Set;

@CacheScoped
public class RetainedMessageLocalXodusV030003Persistence {
    private static final Logger LOGGER = LoggerFactory.getLogger(RetainedMessageLocalXodusV030003Persistence.class);
    public static final String ENTITY_TYPE = "RETAINED_MESSAGE";
    public static final String ENVIRONMENT_NAME = "retained_messages";
    private PersistentEntityStoreImpl persistentEntityStore;
    private final PersistenceFolders persistenceFolders;

    @Inject
    RetainedMessageLocalXodusV030003Persistence(PersistenceFolders persistenceFolders) {
        this.persistenceFolders = persistenceFolders;
    }

    @PostConstruct
    public void init() {
        try {
            EnvironmentConfig config = getConfig(ENVIRONMENT_NAME, 60000);
            this.persistentEntityStore = PersistentEntityStores.newInstance(
                    Environments.newInstance(new File(this.persistenceFolders.root(), ENVIRONMENT_NAME), config));
        } catch (ExodusException e) {
            LOGGER.error("An error occurred while opening the Xodus Retained Message Local persistence. Is another HiveMQ instance running?");
            LOGGER.debug("Original Exception:", e);
            throw new UnrecoverableException(false);
        }
    }

    public void migrate(Callback callback) {
        this.persistentEntityStore.executeInReadonlyTransaction(txn -> {
            EntityIterable foundValues = txn.findWithProp(ENTITY_TYPE, "topic");
            EntityIterable unDeletedValues = txn.find(ENTITY_TYPE, "deleted", false);
            foundValues.intersect(unDeletedValues).forEach(entry -> {
                String topic = (String) entry.getProperty("topic");
                if (topic == null) {
                    return;
                }
                EntityIterable foundValue = txn.find(ENTITY_TYPE, "topic", topic);
                if (!foundValue.isEmpty()) {
                    Entity foundEntry = foundValue.getLast();
                    if (!(Boolean) foundEntry.getProperty("deleted")) {
                        callback.persistent(topic, buildRetainedMessage(foundEntry), (Long) foundEntry.getProperty("ts"));
                    }
                }
            });
        });
    }

    public Long getTimestamp(@NotNull String topic) {
        Preconditions.checkNotNull(topic, "Topic must not be null");
        Long timestamp = this.persistentEntityStore.computeInReadonlyTransaction(txn -> {
            EntityIterable foundValue = txn.find(ENTITY_TYPE, "topic", topic);
            if (foundValue.isEmpty()) {
                return null;
            }
            Entity lastEntry = foundValue.getLast();
            if ((Boolean) lastEntry.getProperty("deleted")) {
                return null;
            }
            return (Long) lastEntry.getProperty("ts");
        });
        LOGGER.trace("Getting Retained Message timestamp for topic {}: {}", topic, timestamp);
        return timestamp;
    }

    public void remove(@NotNull String topic, long timestamp) {
        Preconditions.checkNotNull(topic, "Topic must not be null");
        Preconditions.checkArgument(timestamp > 0L, "Timestamp must be > 0, was %s", timestamp);
        this.persistentEntityStore.executeInTransaction(new RemoveTransactionalExecutable(topic, timestamp));
    }

    public void remove(@NotNull String topic) {
        remove(topic, System.currentTimeMillis());
    }

    public void addOrReplace(@NotNull String topic, long timestamp, @NotNull RetainedMessage retainedMessage) {
        Preconditions.checkNotNull(topic, "Topic must not be null");
        Preconditions.checkArgument(timestamp > 0L, "Timestamp must be > 0, was %s", timestamp);
        this.persistentEntityStore.executeInTransaction(new AddOrReplaceTransactionalExecutable(topic, retainedMessage, timestamp));
    }

    public void addOrReplace(@NotNull String topic, @NotNull RetainedMessage retainedMessage) {
        addOrReplace(topic, System.currentTimeMillis(), retainedMessage);
    }

    @Nullable
    public RetainedMessage get(@NotNull String topic) {
        Preconditions.checkNotNull(topic, "Topic must not be null");
        RetainedMessage retainedMessage = this.persistentEntityStore.computeInReadonlyTransaction(txn -> {
            EntityIterable foundValue = txn.find(ENTITY_TYPE, "topic", topic);
            if (foundValue.isEmpty()) {
                return null;
            }
            Entity entity = foundValue.getLast();
            if ((Boolean) entity.getProperty("deleted")) {
                return null;
            }
            return buildRetainedMessage(entity);
        });
        LOGGER.trace("Getting Retained Message for topic {}: ", topic, retainedMessage);
        return retainedMessage;
    }

    public long size() {
        Long size = this.persistentEntityStore.computeInReadonlyTransaction(txn ->
                txn.find(ENTITY_TYPE, "deleted", false).size());
        LOGGER.trace("Getting size of Retained Message Persistence: {}", size);
        return size;
    }

    public boolean isEmpty() {
        Boolean isEmpty = this.persistentEntityStore.computeInReadonlyTransaction(txn ->
                txn.find(ENTITY_TYPE, "deleted", false).isEmpty());
        LOGGER.trace("Getting if Retained Message Store is empty: {}", isEmpty);
        return isEmpty;
    }

    public void clear() {
        LOGGER.warn("Clearing Retained Message store");
        this.persistentEntityStore.clear();
    }

    public boolean contains(@NotNull String topic) {
        Preconditions.checkNotNull(topic, "Topic must not be null");
        Boolean contains = this.persistentEntityStore.computeInReadonlyTransaction(txn -> {
            EntityIterable foundValue = txn.find(ENTITY_TYPE, "topic", topic);
            if (foundValue.isEmpty()) {
                return false;
            }
            boolean deleted = (Boolean) foundValue.getLast().getProperty("deleted");
            return !deleted;
        });
        LOGGER.trace("Getting if Retained Message Store contains retained message for topic {}: {}",
                topic, contains);
        return contains;
    }

    public Set<String> getAll() {
        Set<String> topics = this.persistentEntityStore.computeInReadonlyTransaction(txn -> {
            EntityIterable foundValues = txn.findWithProp(ENTITY_TYPE, "topic");
            EntityIterable unDeletedValues = txn.find(ENTITY_TYPE, "deleted", false);
            ImmutableSet.Builder<String> builder = ImmutableSet.builder();
            foundValues.intersect(unDeletedValues).forEach(entry ->
                    builder.add((String) entry.getProperty("topic"))
            );
            return builder.build();
        });
        LOGGER.trace("Getting if all Retained Message Topics. Size {}", topics.size());
        return topics;
    }

    @VisibleForTesting
    PersistentEntityStoreImpl getPersistentEntityStore() {
        return this.persistentEntityStore;
    }


    @Nullable
    private static RetainedMessage buildRetainedMessage(@NotNull Entity entity) {
        Preconditions.checkNotNull(entity, "Entity must not be null");
        try {
            byte[] message = ByteStreams.toByteArray(entity.getBlob("value"));
            int qoSNumber = (Integer) entity.getProperty("qos");
            return new RetainedMessage(message, QoS.valueOf(qoSNumber));
        } catch (IOException e) {
            LOGGER.error("Could not create Retained message from entity {}", entity, e);
        }
        return null;
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
        void persistent(String topic, RetainedMessage retainedMessage, Long timestamp);
    }


    static class AddOrReplaceTransactionalExecutable
            implements StoreTransactionalExecutable {
        private final String topic;
        private final RetainedMessage retainedMessage;
        private final long timestamp;

        public AddOrReplaceTransactionalExecutable(String topic,
                                                   RetainedMessage retainedMessage,
                                                   long timestamp) {
            this.topic = topic;
            this.retainedMessage = retainedMessage;
            this.timestamp = timestamp;
        }

        public void execute(StoreTransaction txn) {
            EntityIterable entityIterable = txn.find(ENTITY_TYPE, "topic", this.topic);
            long size = entityIterable.size();
            if (size == 1L) {
                addOrReplace(entityIterable);
            } else if (size == 0L) {
                LOGGER.trace("Creating new retained message for topic {} (qos: {}, size: {}) and timestamp: {}",
                        this.topic, this.retainedMessage.getQoS().getQosNumber(), this.retainedMessage.getMessage().length, this.timestamp);
                Entity newEntity = txn.newEntity(ENTITY_TYPE);
                newEntity.setProperty("topic", this.topic);
                setEntry(newEntity);
            } else {
                LOGGER.error("More than one retained message ({}) for topic {} was found. Using last entry. This is a severe error since data could be inconsistent.");
                addOrReplace(entityIterable);
            }
        }

        private void addOrReplace(EntityIterable entityIterable) {
            Entity lastEntry = entityIterable.getLast();
            long ts = (Long) lastEntry.getProperty("ts");
            if (ts <= this.timestamp) {
                LOGGER.trace("Replacing retained message for topic {} (qos: {}, size: {}). Old timestamp: {}, new timestamp: {}",
                        this.topic, this.retainedMessage.getQoS().getQosNumber(), this.retainedMessage.getMessage().length, ts, this.timestamp);
                setEntry(lastEntry);
            } else {
                LOGGER.trace("Retained message for topic {} (qos: {}, size: {}) was not added because the timestamp ({}) is newer than the new entries timestamp ({})",
                        this.topic, this.retainedMessage.getQoS().getQosNumber(), this.retainedMessage.getMessage().length, ts, this.timestamp);
            }
        }

        private void setEntry(Entity entity) {
            entity.setProperty("qos", this.retainedMessage.getQoS().getQosNumber());
            entity.setProperty("ts", this.timestamp);
            entity.setProperty("deleted", false);
            entity.setBlob("value", new ByteArrayInputStream(this.retainedMessage.getMessage()));
        }
    }


    static class RemoveTransactionalExecutable
            implements StoreTransactionalExecutable {
        private final String topic;
        private final long timestamp;

        public RemoveTransactionalExecutable(String topic, long timestamp) {
            this.topic = topic;
            this.timestamp = timestamp;
        }

        public void execute(StoreTransaction txn) {
            EntityIterable foundEntities = txn.find(ENTITY_TYPE, "topic", this.topic);
            long size = foundEntities.size();
            if (size == 1L) {
                remove(foundEntities);
            } else if (size == 0L) {
                LOGGER.trace("Failed to mark retained message for topic {} as deleted because no retained message is stored for this topic",
                        this.topic);
            } else {
                LOGGER.error("More than one retained message ({}) for topic {} was found. Using last entry. This is a severe error since data could be inconsistent.");
                remove(foundEntities);
            }
        }


        private void remove(EntityIterable foundEntities) {
            Entity lastEntity = foundEntities.getLast();
            long foundTimestamp = (Long) lastEntity.getProperty("ts");
            if (foundTimestamp >= this.timestamp) {
                LOGGER.trace("Retained message for topic {} was not removed because the timestamp ({}) is newer than the new entries timestamp ({})",
                        this.topic, foundTimestamp, this.timestamp);
                return;
            }
            LOGGER.trace("Marking retained message for topic {} was removed. Old timestamp: {}, new timestamp: {}",
                    this.topic, foundTimestamp, this.timestamp);
            lastEntity.deleteBlob("value");
            lastEntity.deleteProperty("qos");
            lastEntity.setProperty("ts", this.timestamp);
            lastEntity.setProperty("deleted", true);
        }
    }
}
