package bn1;

import ak.VectorClock;
import av.InternalConfigurationService;
import av.Internals;
import av.PersistenceConfigurationService;
import av.PersistenceConfigurationService.PersistenceMode;
import bc1.OutgoingMessageFlowLocalPersistence;
import bg1.OutgoingMessageFlowLocalMemoryPersistence;
import bg1.OutgoingMessageFlowLocalXodusPersistence;
import bl1.ChannelPersistence;
import cb1.AttributeKeys;
import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.hivemq.spi.annotations.NotNull;
import com.hivemq.spi.message.MessageWithId;
import d.CacheScoped;
import io.netty.channel.Channel;
import u.Filter;
import u.PersistenceExecutor;
import u1.OutgoingMessageFlowPutReplicateRequest;
import u1.OutgoingMessageFlowPutRequest;
import u1.OutgoingMessageFlowRemoveAllReplicateRequest;
import u1.OutgoingMessageFlowReplicateRequest;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

@CacheScoped
public class OutgoingMessageFlowSinglePersistenceImpl
        implements OutgoingMessageFlowClusterPersistence, OutgoingMessageFlowSinglePersistence {
    private final OutgoingMessageFlowLocalPersistence localPersistence;
    private final MetricRegistry metricRegistry;
    private final InternalConfigurationService internalConfigurationService;
    private final PersistenceConfigurationService persistenceConfigurationService;
    private final ChannelPersistence channelPersistence;
    private final OutgoingMessageFlowLocalMemoryPersistence localMemoryPersistence;
    private final int bucketCount;
    private final List<PersistenceExecutor> persistenceExecutors;

    @Inject
    OutgoingMessageFlowSinglePersistenceImpl(OutgoingMessageFlowLocalPersistence localPersistence,
                                             MetricRegistry metricRegistry,
                                             InternalConfigurationService internalConfigurationService,
                                             PersistenceConfigurationService persistenceConfigurationService,
                                             ChannelPersistence channelPersistence,
                                             OutgoingMessageFlowLocalMemoryPersistence localMemoryPersistence) {
        this.localPersistence = localPersistence;
        this.metricRegistry = metricRegistry;
        this.internalConfigurationService = internalConfigurationService;
        this.persistenceConfigurationService = persistenceConfigurationService;
        this.channelPersistence = channelPersistence;
        this.localMemoryPersistence = localMemoryPersistence;
        this.bucketCount = this.internalConfigurationService.getInt(Internals.PERSISTENCE_OUTGOING_MESSAGE_FLOW_BUCKET_COUNT);
        this.persistenceExecutors = new ArrayList<>(this.bucketCount);
        for (int bucket = 0; bucket < this.bucketCount; bucket++) {
            this.persistenceExecutors.add(new PersistenceExecutor("outgoing-message-flow-writer-" + bucket, metricRegistry));
        }
    }


    private PersistenceExecutor getPersistenceExecutor(String clientId) {
        int bucket = Math.abs(clientId.hashCode() % OutgoingMessageFlowLocalXodusPersistence.BUCKET_COUNT % this.bucketCount);
        return this.persistenceExecutors.get(bucket);
    }

    private OutgoingMessageFlowLocalPersistence getLocalPersistence(String clientId) {
        if (this.persistenceConfigurationService.getMessageFlowOutgoingMode() == PersistenceMode.FILE) {
            return this.localPersistence;
        }
        Channel channel = this.channelPersistence.getChannel(clientId);
        if (channel == null) {
            return this.localPersistence;
        }
        Boolean persistentSession = channel.attr(AttributeKeys.MQTT_PERSISTENT_SESSION).get();
        if (persistentSession != null && persistentSession) {
            return this.localPersistence;
        }
        return this.localMemoryPersistence;
    }

    public MessageWithId get(String clientId, int messageId) {
        return getLocalPersistence(clientId).get(clientId, messageId);
    }

    public ListenableFuture<Void> addOrReplace(String clientId, int messageId, MessageWithId message) {
        return getPersistenceExecutor(clientId).add(() -> {
            getLocalPersistence(clientId).addOrReplace(clientId, messageId, message);
            return null;
        });
    }

    public ListenableFuture<MessageWithId> remove(String clientId, int messageId) {
        return getPersistenceExecutor(clientId).add(() ->
                getLocalPersistence(clientId).remove(clientId, messageId)
        );
    }

    public ListenableFuture<Void> removeForClient(String clientId) {
        return getPersistenceExecutor(clientId).add(() -> {
            localPersistence.removeAll(clientId);
            localMemoryPersistence.removeAll(clientId);
            return null;
        });
    }

    public int size(String clientId) {
        return getLocalPersistence(clientId).size(clientId);
    }

    public ImmutableList<MessageWithId> getAll(@NotNull String clientId) {
        return getLocalPersistence(clientId).drain(clientId);
    }

    public ListenableFuture<Void> add(String clientId, MessageWithId message) {
        throw new UnsupportedOperationException("The method 'add' is not supported in single mode.");
    }

    public ListenableFuture<Void> handlePutRequest(OutgoingMessageFlowPutRequest request, String connectedNode) {
        throw new UnsupportedOperationException("The method 'handlePutRequest' is not supported in single mode.");
    }

    public ListenableFuture<Void> replicateClientFlow(String clientId, ImmutableList<MessageWithId> messages, VectorClock requestVectorClock) {
        throw new UnsupportedOperationException("The method 'replicateClientFlow' is not supported in single mode.");
    }

    public ListenableFuture<Void> handlePutReplica(OutgoingMessageFlowPutReplicateRequest request) {
        throw new UnsupportedOperationException("The method 'handlePutReplica' is not supported in single mode.");
    }

    public ListenableFuture<Void> removeAll(@NotNull String clientId) {
        throw new UnsupportedOperationException("The method 'removeAll' is not supported in single mode.");
    }

    public ListenableFuture<Void> replicateRemoveAll(String clientId, VectorClock requestVectorClock) {
        throw new UnsupportedOperationException("The method 'replicateRemoveAll' is not supported in single mode.");
    }

    public ListenableFuture<Void> handleRemoveAllReplica(OutgoingMessageFlowRemoveAllReplicateRequest request) {
        throw new UnsupportedOperationException("The method 'handleRemoveAllReplica' is not supported in single mode.");
    }

    public ListenableFuture<Void> put(@NotNull String clientId) {
        return Futures.immediateFuture(null);
    }

    public ListenableFuture<OutgoingMessageFlowReplicateRequest> getLocalData(@NotNull Filter filter) {
        throw new UnsupportedOperationException("The method 'getLocalData' is not supported in single mode.");
    }

    public ListenableFuture<Void> removeLocally(Filter filter) {
        throw new UnsupportedOperationException("The method 'removeLocally' is not supported in single mode.");
    }

    public ListenableFuture<Void> processReplicationRequest(@NotNull OutgoingMessageFlowReplicateRequest request) {
        throw new UnsupportedOperationException("The method 'processReplicationRequest' is not supported in single mode.");
    }
}
