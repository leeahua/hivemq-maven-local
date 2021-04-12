package bm1;

import ak.VectorClock;
import bc1.ClientSessionQueueEntry;
import bc1.QueuedMessagesLocalPersistence;
import bu.InternalPublish;
import com.codahale.metrics.MetricRegistry;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ListenableFuture;
import com.hivemq.spi.annotations.NotNull;
import d.CacheScoped;
import u.Filter;
import u.PersistenceExecutor;
import v1.MessageQueueReplicateRequest;
import w.QueuedMessagesClusterPersistence;

import javax.inject.Inject;

@CacheScoped
public class QueuedMessagesSinglePersistenceImpl
        implements QueuedMessagesSinglePersistence, QueuedMessagesClusterPersistence {
    private final QueuedMessagesLocalPersistence queuedMessagesLocalPersistence;
    private final PersistenceExecutor persistenceExecutor;
    private final MetricRegistry metricRegistry;

    @Inject
    public QueuedMessagesSinglePersistenceImpl(
            QueuedMessagesLocalPersistence queuedMessagesLocalPersistence,
            MetricRegistry metricRegistry) {
        this.queuedMessagesLocalPersistence = queuedMessagesLocalPersistence;
        this.metricRegistry = metricRegistry;
        this.persistenceExecutor = new PersistenceExecutor("message-queue-writer", this.metricRegistry);
    }

    public ListenableFuture<Void> offer(String clientId, InternalPublish publish) {
        return this.persistenceExecutor.add(() -> {
            queuedMessagesLocalPersistence.offer(clientId, publish, System.currentTimeMillis());
            return null;
        });
    }

    public ListenableFuture<Boolean> queuePublishIfQueueNotEmpty(@NotNull String clientId, @NotNull InternalPublish publish) {
        return this.persistenceExecutor.add(
                () -> queuedMessagesLocalPersistence.queuePublishIfQueueNotEmpty(clientId, publish));
    }

    public ListenableFuture<InternalPublish> poll(String clientId) {
        return this.persistenceExecutor.add(() -> {
            ClientSessionQueueEntry entry = queuedMessagesLocalPersistence.poll(clientId, System.currentTimeMillis());
            if (entry == null) {
                return null;
            }
            InternalPublish publish = new InternalPublish(entry.getSequence(), entry.getTimestamp(), entry.getClusterId());
            publish.setTopic(entry.getTopic());
            publish.setQoS(entry.getQoS());
            publish.setPayload(entry.getPayload());
            return publish;
        });
    }

    public ListenableFuture<Void> remove(String clientId) {
        return this.persistenceExecutor.add(() -> {
            queuedMessagesLocalPersistence.remove(clientId, System.currentTimeMillis());
            return null;
        });
    }

    public ListenableFuture<Void> cleanUp(long tombstoneMaxAge) {
        return this.persistenceExecutor.add(() -> {
            queuedMessagesLocalPersistence.cleanUp(tombstoneMaxAge);
            return null;
        });
    }

    public boolean isEmpty(String clientId) {
        Preconditions.checkNotNull(clientId, "Client id must not be null");
        return this.queuedMessagesLocalPersistence.size(clientId) == 0L;
    }

    public boolean b(@NotNull String clientId) {
        return this.queuedMessagesLocalPersistence.size(clientId) == 0L;
    }

    public ListenableFuture<Void> replicateOffer(@NotNull String clientId, @NotNull InternalPublish publish, long requestTimestamp, @NotNull VectorClock requestVectorClock) {
        throw new UnsupportedOperationException("The 'replicateOffer' method is not supported in single mode.");
    }

    public ListenableFuture<Void> processOfferReplica(@NotNull String clientId, @NotNull InternalPublish publish, long requestTimestamp, @NotNull VectorClock requestVectorClock) {
        throw new UnsupportedOperationException("The 'processOfferReplica' method is not supported in single mode.");
    }

    public ListenableFuture<InternalPublish> processPollRequest(@NotNull String clientId, @NotNull String requestId, @NotNull String sender) {
        throw new UnsupportedOperationException("The 'processPollRequest' method is not supported in single mode.");
    }

    public ListenableFuture<Void> processRemoveReplica(@NotNull String clientId, long entryTimestamp, @NotNull String entryId, @NotNull VectorClock requestVectorClock, long requestTimestamp) {
        throw new UnsupportedOperationException("The 'processRemoveReplica' method is not supported in single mode.");
    }

    public ListenableFuture<Void> replicateRemove(@NotNull String clientId, long entryTimestamp, @NotNull String entryId, @NotNull VectorClock requestVectorClock, long requestTimestamp) {
        throw new UnsupportedOperationException("The 'replicateRemove' method is not supported in single mode.");
    }

    public ListenableFuture<Void> processRemoveAllReplica(@NotNull String clientId, long requestTimestamp, @NotNull VectorClock requestVectorClock) {
        throw new UnsupportedOperationException("The 'processRemoveAllReplica' method is not supported in single mode.");
    }

    public ListenableFuture<Void> replicateRemoveAll(String clientId, VectorClock requestVectorClock, long requestTimestamp) {
        throw new UnsupportedOperationException("The 'replicateRemoveAll' method is not supported in single mode.");
    }

    public void processAckDrained(@NotNull String clientId, @NotNull String sender) {
        throw new UnsupportedOperationException("The 'processAckDrained' method is not supported in single mode.");
    }

    public ListenableFuture<MessageQueueReplicateRequest> getDataForReplica(@NotNull Filter filter) {
        throw new UnsupportedOperationException("The 'getDataForReplica' method is not supported in single mode.");
    }

    public ListenableFuture<Void> processReplicationRequest(@NotNull MessageQueueReplicateRequest request) {
        throw new UnsupportedOperationException("The 'processReplicationRequest' method is not supported in single mode.");
    }

    public ListenableFuture<Void> ackDrained(@NotNull String clientId) {
        throw new UnsupportedOperationException("The 'ackDrained' method is not supported in single mode.");
    }

    public void removeLocally(@NotNull Filter filter) {
        throw new UnsupportedOperationException("The 'removeLocally' method is not supported in single mode.");
    }

    public void resetQueueDrained(@NotNull String clientId) {
        throw new UnsupportedOperationException("The 'resetQueueDrained' method is not supported in single mode.");
    }
}
