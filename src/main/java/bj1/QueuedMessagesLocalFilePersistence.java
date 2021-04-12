package bj1;

import av.PersistenceConfigurationService;
import bc1.ClientSessionQueueEntry;
import bc1.QueuedMessagesLocalPersistence;
import bu.InternalPublish;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.hivemq.spi.annotations.NotNull;
import d.CacheScoped;
import u.TimestampObject;
import u.Filter;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Provider;
import java.util.Set;

@CacheScoped
public class QueuedMessagesLocalFilePersistence
        implements QueuedMessagesLocalPersistence {
    private final Provider<QueuedMessagesLocalEmptyPersistence> emptyPersistenceProvider;
    private final Provider<QueuedMessagesLocalXodusPersistence> exodusPersistenceProvider;
    private final PersistenceConfigurationService persistenceConfigurationService;
    private QueuedMessagesLocalPersistence persistence;

    @Inject
    QueuedMessagesLocalFilePersistence(
            Provider<QueuedMessagesLocalEmptyPersistence> emptyPersistenceProvider,
            Provider<QueuedMessagesLocalXodusPersistence> exodusPersistenceProvider,
            PersistenceConfigurationService persistenceConfigurationService) {
        this.emptyPersistenceProvider = emptyPersistenceProvider;
        this.exodusPersistenceProvider = exodusPersistenceProvider;
        this.persistenceConfigurationService = persistenceConfigurationService;
        setPersistence();
    }

    @PostConstruct
    void init() {
        this.persistenceConfigurationService.register(newValue -> setPersistence());
    }

    private void setPersistence() {
        long maxQueuedMessages = this.persistenceConfigurationService.getMaxQueuedMessages();
        if (maxQueuedMessages == 0L) {
            this.persistence = this.emptyPersistenceProvider.get();
        } else {
            this.persistence = this.exodusPersistenceProvider.get();
        }
    }

    public void clear() {
        this.persistence.clear();
    }

    public void close() {
        this.persistence.close();
    }

    public void offer(String clientId, InternalPublish publish, long timestamp) {
        this.persistence.offer(clientId, publish, timestamp);
    }

    public void remove(String clientId, String entryId, long entryTimestamp, long timestamp) {
        this.persistence.remove(clientId, entryId, entryTimestamp, timestamp);
    }

    public boolean queuePublishIfQueueNotEmpty(@NotNull String clientId, @NotNull InternalPublish publish) {
        return this.persistence.queuePublishIfQueueNotEmpty(clientId, publish);
    }

    public ClientSessionQueueEntry poll(String clientId, long timestamp) {
        return this.persistence.poll(clientId, timestamp);
    }

    public void remove(String clientId, long timestamp) {
        this.persistence.remove(clientId, timestamp);
    }

    public long size(String clientId) {
        return this.persistence.size(clientId);
    }

    public ClientSessionQueueEntry peek(String clientId) {
        return this.persistence.peek(clientId);
    }

    public ImmutableMap<String, TimestampObject<Set<ClientSessionQueueEntry>>> getEntries(Filter filter) {
        return this.persistence.getEntries(filter);
    }

    public void offerAll(ImmutableSet<ClientSessionQueueEntry> queueEntries, String clientId, long timestamp) {
        this.persistence.offerAll(queueEntries, clientId, timestamp);
    }

    public ImmutableSet<String> cleanUp(long tombstoneMaxAge) {
        return this.persistence.cleanUp(tombstoneMaxAge);
    }

    public long getTimestamp(String clientId) {
        return this.persistence.getTimestamp(clientId);
    }

    public ImmutableSet<String> remove(Filter filter) {
        return this.persistence.remove(filter);
    }
}
