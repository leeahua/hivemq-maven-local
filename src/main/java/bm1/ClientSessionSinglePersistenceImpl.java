package bm1;

import aj.ClusterFutures;
import ak.VectorClock;
import am1.Metrics;
import av.InternalConfigurationService;
import av.Internals;
import bc1.ClientSessionLocalPersistence;
import bh1.BucketUtils;
import bn1.OutgoingMessageFlowSinglePersistence;
import bu.MessageIDPools;
import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.hivemq.spi.annotations.NotNull;
import d.CacheScoped;
import i.ClusterIdProducer;
import t1.ClientSessionReplicateRequest;
import u.Filter;
import u.NotImplementedException;
import u.PersistenceExecutor;
import u.TimestampObject;
import u1.MessageFlow;
import v.ClientSession;
import v.ClientSessionClusterPersistence;
import w.QueuedMessagesClusterPersistence;
import y.ClientSessionSubscriptionsClusterPersistence;
import y.ClientSubscriptions;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;


@CacheScoped
public class ClientSessionSinglePersistenceImpl
        implements ClientSessionSinglePersistence, ClientSessionClusterPersistence {
    private final ClientSessionLocalPersistence clientSessionLocalPersistence;
    private final QueuedMessagesClusterPersistence queuedMessagesClusterPersistence;
    private final ClientSessionSubscriptionsClusterPersistence clientSessionSubscriptionsClusterPersistence;
    private final OutgoingMessageFlowSinglePersistence outgoingMessageFlowSinglePersistence;
    private final MessageIDPools messageIDPools;
    private final ClusterIdProducer clusterIdProducer;
    private final Metrics metrics;
    private final InternalConfigurationService internalConfigurationService;
    private final MetricRegistry metricRegistry;
    private List<PersistenceExecutor> persistenceExecutors;
    private int bucketCount;

    @Inject
    public ClientSessionSinglePersistenceImpl(ClientSessionLocalPersistence clientSessionLocalPersistence,
                                              ClusterIdProducer clusterIdProducer,
                                              OutgoingMessageFlowSinglePersistence outgoingMessageFlowSinglePersistence,
                                              QueuedMessagesClusterPersistence queuedMessagesClusterPersistence,
                                              ClientSessionSubscriptionsClusterPersistence clientSessionSubscriptionsClusterPersistence,
                                              MessageIDPools messageIDPools,
                                              Metrics metrics,
                                              InternalConfigurationService internalConfigurationService,
                                              MetricRegistry metricRegistry) {
        this.clientSessionLocalPersistence = clientSessionLocalPersistence;
        this.queuedMessagesClusterPersistence = queuedMessagesClusterPersistence;
        this.clientSessionSubscriptionsClusterPersistence = clientSessionSubscriptionsClusterPersistence;
        this.clusterIdProducer = clusterIdProducer;
        this.outgoingMessageFlowSinglePersistence = outgoingMessageFlowSinglePersistence;
        this.messageIDPools = messageIDPools;
        this.metrics = metrics;
        this.internalConfigurationService = internalConfigurationService;
        this.metricRegistry = metricRegistry;
    }

    @PostConstruct
    protected void init() {
        this.bucketCount = this.internalConfigurationService.getInt(Internals.PERSISTENCE_CLIENT_SESSIONS_BUCKET_COUNT);
        this.persistenceExecutors = new ArrayList<>();
        for (int bucket = 0; bucket < this.bucketCount; bucket++) {
            this.persistenceExecutors.add(new PersistenceExecutor("client-session-writer-" + bucket, this.metricRegistry));
        }
    }

    public ListenableFuture<Boolean> isPersistent(@NotNull String clientId) {
        ClientSession clientSession = this.clientSessionLocalPersistence.get(clientId);
        return Futures.immediateFuture(clientSession != null &&
                (clientSession.isPersistentSession() || clientSession.isConnected()));
    }

    public ListenableFuture<Void> disconnected(String clientId) {
        long timestamp = System.currentTimeMillis();
        return getExecutor(clientId).add(() -> {
            ClientSession clientSession = clientSessionLocalPersistence.disconnect(clientId, clusterIdProducer.get(), timestamp);
            if (clientSession.isPersistentSession()) {
                return null;
            }
            messageIDPools.remove(clientId);
            queuedMessagesClusterPersistence.remove(clientId);
            ClientSubscriptions clientSubscriptions = clientSessionSubscriptionsClusterPersistence.getClientSubscriptions(clientId);
            metrics.subscriptionsCurrent().dec(clientSubscriptions.getSubscriptions().size());
            clientSessionSubscriptionsClusterPersistence.removeAllLocally(clientId);
            outgoingMessageFlowSinglePersistence.removeForClient(clientId);
            return null;
        });
    }

    public ListenableFuture<MessageFlow> persistent(String clientId, boolean persistentSession) {
        ClientSession clientSession = new ClientSession(true, persistentSession, this.clusterIdProducer.get());
        return getExecutor(clientId).add(() -> {
            clientSessionLocalPersistence.persistent(clientId, clientSession, System.currentTimeMillis());
            if (!persistentSession) {
                messageIDPools.remove(clientId);
                queuedMessagesClusterPersistence.remove(clientId);
                ClientSubscriptions value = clientSessionSubscriptionsClusterPersistence.getClientSubscriptions(clientId);
                metrics.subscriptionsCurrent().dec(value.getSubscriptions().size());
                clientSessionSubscriptionsClusterPersistence.removeAllLocally(clientId);
                outgoingMessageFlowSinglePersistence.removeForClient(clientId);
            }
            return new MessageFlow(outgoingMessageFlowSinglePersistence.getAll(clientId));
        });
    }

    public ListenableFuture<Set<String>> getLocalDisconnectedClients() {
        List<ListenableFuture<Map<String, TimestampObject<ClientSession>>>> futures = new ArrayList();
        for (int bucket = 0; bucket < this.bucketCount; bucket++) {
            int bucketIndex = bucket;
            futures.add((this.persistenceExecutors.get(bucket)).add(() ->
                    clientSessionLocalPersistence.get(clientId -> true, bucketIndex)
            ));
        }
        SettableFuture<Set<String>> settableFuture = SettableFuture.create();
        ClusterFutures.addCallback(Futures.allAsList(futures), new FutureCallback<List<Map<String, TimestampObject<ClientSession>>>>() {

            @Override
            public void onSuccess(@Nullable List<Map<String, TimestampObject<ClientSession>>> result) {
                if (result == null) {
                    settableFuture.set(Collections.emptySet());
                    return;
                }
                Set<String> disconnectedClients = result.stream()
                        .flatMap(map -> map.entrySet().stream())
                        .filter(entry -> entry.getValue().getObject() != null &&
                                !entry.getValue().getObject().isConnected())
                        .map(Map.Entry::getKey)
                        .collect(Collectors.toSet());
                settableFuture.set(disconnectedClients);
            }

            @Override
            public void onFailure(Throwable t) {
                settableFuture.setException(t);
            }
        });
        return settableFuture;
    }

    public ListenableFuture<Set<String>> getAllClients() {
        return getLocalAllClients();
    }

    public ListenableFuture<Set<String>> getLocalAllClients() {
        List<ListenableFuture<Map<String, TimestampObject<ClientSession>>>> futures = new ArrayList();
        for (int bucket = 0; bucket < this.bucketCount; bucket++) {
            int bucketIndex = bucket;
            futures.add(this.persistenceExecutors.get(bucket).add(() ->
                    clientSessionLocalPersistence.get(clientId -> true, bucketIndex)
            ));
        }
        SettableFuture<Set<String>> settableFuture = SettableFuture.create();
        ClusterFutures.addCallback(Futures.allAsList(futures), new FutureCallback<List<Map<String, TimestampObject<ClientSession>>>>() {
            @Override
            public void onSuccess(@Nullable List<Map<String, TimestampObject<ClientSession>>> result) {
                if (result == null) {
                    settableFuture.set(Collections.emptySet());
                    return;
                }
                Set<String> clients = result.stream()
                        .flatMap(map -> map.entrySet().stream())
                        .filter(entry -> Objects.nonNull(entry.getValue().getObject()))
                        .map(Map.Entry::getKey)
                        .collect(Collectors.toSet());
                settableFuture.set(clients);
            }

            @Override
            public void onFailure(Throwable t) {
                settableFuture.setException(t);
            }
        });
        return settableFuture;
    }

    public ListenableFuture<Void> cleanUp(long tombstoneMaxAge) {
        List<ListenableFuture<Void>> futures = new ArrayList();
        for (int bucket = 0; bucket < this.bucketCount; bucket++) {
            int bucketIndex = bucket;
            futures.add(this.persistenceExecutors.get(bucket).add(() -> {
                clientSessionLocalPersistence.cleanUp(tombstoneMaxAge, bucketIndex);
                return null;
            }));
        }
        return ClusterFutures.merge(futures);
    }

    private PersistenceExecutor getExecutor(String clientId) {
        return this.persistenceExecutors.get(BucketUtils.bucket(clientId, this.bucketCount));
    }

    public ListenableFuture<String> handleNodeForPublishRequest(@NotNull String clientId) {
        throw new NotImplementedException("Cluster persistence method 'handleNodeForPublishRequest' not implemented in single instance persistence");
    }

    public ListenableFuture<Void> disconnectAllClients(String node) {
        throw new NotImplementedException("Cluster persistence method 'disconnectAllClients' not implemented in single instance persistence");
    }

    public ListenableFuture<String> nodeForPublish(@NotNull String clientId, ExecutorService executorService) {
        throw new NotImplementedException("Cluster persistence method 'nodeForPublish' not implemented in single instance persistence");
    }

    public ListenableFuture<ClientSession> requestSession(@NotNull String clientId) {
        throw new NotImplementedException("Cluster persistence method 'requestSession' not implemented in single instance persistence");
    }

    public ListenableFuture<ClientSession> getLocally(@NotNull String clientId) {
        throw new NotImplementedException("Cluster persistence method 'getLocally' not implemented in single instance persistence");
    }

    public ListenableFuture<MessageFlow> clientConnectedRequest(@NotNull String clientId, boolean persistentSession, @NotNull String connectedNode) {
        throw new NotImplementedException("Cluster persistence method 'clientConnectedRequest' not implemented in single instance persistence");
    }

    public ListenableFuture<Void> clientDisconnectedRequest(@NotNull String clientId, @NotNull String connectedNode) {
        throw new NotImplementedException("Cluster persistence method 'clientDisconnectedRequest' not implemented in single instance persistence");
    }

    public ListenableFuture<Void> replicateClientSession(@NotNull String paramString, @NotNull ClientSession clientSession, long requestTimestamp, VectorClock requestVectorClock) {
        throw new NotImplementedException("Cluster persistence method 'replicateClientSession' not implemented in single instance persistence");
    }

    public ListenableFuture<ImmutableSet<ClientSessionReplicateRequest>> getDataForReplica(@NotNull Filter filter) {
        throw new NotImplementedException("Cluster persistence method 'getDataForReplica' not implemented in single instance persistence");
    }

    public ListenableFuture<Void> removeLocally(@NotNull Filter filter) {
        throw new NotImplementedException("Cluster persistence method 'removeLocally' not implemented in single instance persistence");
    }

    public ListenableFuture<Void> handleReplica(@NotNull String clientId, @NotNull ClientSession clientSession, long requestTimestamp, VectorClock requestVectorClock) {
        throw new NotImplementedException("Cluster persistence method 'handleReplica' not implemented in single instance persistence");
    }

    public ListenableFuture<Void> handleMergeReplica(@NotNull String clientId, @NotNull ClientSession parama, long timestamp) {
        throw new NotImplementedException("Cluster persistence method 'handleMergeReplica' not implemented in single instance persistence");
    }
}
