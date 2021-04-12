package bn1;

import aj.ClusterFutures;
import ak.VectorClock;
import av.InternalConfigurationService;
import av.Internals;
import av.PersistenceConfigurationService;
import av.PersistenceConfigurationService.PersistenceMode;
import bc1.OutgoingMessageFlowLocalPersistence;
import bg1.OutgoingMessageFlowLocalMemoryPersistence;
import bg1.OutgoingMessageFlowLocalXodusPersistence;
import bl1.ChannelPersistence;
import bm1.ClientSessionClusterPersistenceProvider;
import bu.InternalPublish;
import cb1.AttributeKeys;
import com.codahale.metrics.MetricRegistry;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableScheduledFuture;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.SettableFuture;
import com.hivemq.spi.annotations.NotNull;
import com.hivemq.spi.annotations.Nullable;
import com.hivemq.spi.message.MessageWithId;
import com.hivemq.spi.message.PubRel;
import d.CacheScoped;
import i.ClusterConfigurationService;
import i.PublishDispatcher;
import io.netty.channel.Channel;
import j1.ClusterKeyRequest;
import j1.ClusterRequestFuture;
import m1.OutgoingMessageFlowPutReplicateRequestCallback;
import m1.OutgoingMessageFlowPutRequestCallback;
import m1.OutgoingMessageFlowRemoveAllReplicateRequestCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import q.ConsistentHashingRing;
import s.Cluster;
import s.Primary;
import t.ClusterConnection;
import t.ClusterResponseHandler;
import u.Filter;
import u.PersistenceExecutor;
import u.VectorClocks;
import u1.OutgoingMessageFlowPutReplicateRequest;
import u1.OutgoingMessageFlowPutRequest;
import u1.OutgoingMessageFlowRemoveAllReplicateRequest;
import u1.OutgoingMessageFlowReplicateRequest;
import v.ClientSession;
import v.ClientSessionClusterPersistence;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

// TODO:
@CacheScoped
public class OutgoingMessageFlowClusterPersistenceImpl
        implements OutgoingMessageFlowClusterPersistence, OutgoingMessageFlowSinglePersistence {
    private static final Logger LOGGER = LoggerFactory.getLogger(OutgoingMessageFlowClusterPersistenceImpl.class);
    private final VectorClocks vectorClocks;
    private final ConsistentHashingRing primaryRing;
    private final OutgoingMessageFlowLocalMemoryPersistence localMemoryPersistence;
    private final OutgoingMessageFlowLocalPersistence localPersistence;
    private final ClusterConnection clusterConnection;
    private final ListeningScheduledExecutorService clusterScheduledExecutorService;
    private final ClientSessionClusterPersistenceProvider clientSessionLookupTableProvider;
    private final Provider<PublishDispatcher> publishDispatcherProvider;
    private final MetricRegistry metricRegistry;
    private final int replicationInterval;
    private final int replicateCount;
    private final Map<String, ListenableScheduledFuture> clientScheduledFutures = new ConcurrentHashMap<>();
    private final ChannelPersistence channelPersistence;
    private final InternalConfigurationService internalConfigurationService;
    private final List<PersistenceExecutor> persistenceExecutors;
    private final PersistenceConfigurationService persistenceConfigurationService;
    private final int bucketCount;

    @Inject
    OutgoingMessageFlowClusterPersistenceImpl(VectorClocks vectorClocks,
                                              OutgoingMessageFlowLocalPersistence localPersistence,
                                              ClusterConnection clusterConnection,
                                              @Cluster ListeningScheduledExecutorService clusterScheduledExecutor,
                                              ClusterConfigurationService clusterConfigurationService,
                                              @Primary ConsistentHashingRing primaryRing,
                                              OutgoingMessageFlowLocalMemoryPersistence localMemoryPersistence,
                                              ClientSessionClusterPersistenceProvider clientSessionLookupTableProvider,
                                              Provider<PublishDispatcher> publishDispatcherProvider,
                                              MetricRegistry metricRegistry,
                                              ChannelPersistence channelPersistence,
                                              InternalConfigurationService internalConfigurationService,
                                              PersistenceConfigurationService persistenceConfigurationService) {
        this.vectorClocks = vectorClocks;
        this.localPersistence = localPersistence;
        this.clusterConnection = clusterConnection;
        this.clusterScheduledExecutorService = clusterScheduledExecutor;
        this.primaryRing = primaryRing;
        this.localMemoryPersistence = localMemoryPersistence;
        this.clientSessionLookupTableProvider = clientSessionLookupTableProvider;
        this.publishDispatcherProvider = publishDispatcherProvider;
        this.metricRegistry = metricRegistry;
        this.channelPersistence = channelPersistence;
        this.internalConfigurationService = internalConfigurationService;
        this.persistenceConfigurationService = persistenceConfigurationService;
        this.replicateCount = clusterConfigurationService.getReplicates().getOutgoingMessageFlow().getReplicateCount();
        this.replicationInterval = clusterConfigurationService.getReplicates().getOutgoingMessageFlow().getReplicationInterval();
        this.bucketCount = this.internalConfigurationService.getInt(Internals.PERSISTENCE_OUTGOING_MESSAGE_FLOW_BUCKET_COUNT);
        this.persistenceExecutors = new ArrayList<>(this.bucketCount);
        for (int bucket = 0; bucket < this.bucketCount; bucket++) {
            this.persistenceExecutors.add(new PersistenceExecutor("outgoing-message-flow-writer-" + bucket, metricRegistry));
        }
    }

    private PersistenceExecutor getPersistenceExecutor(String clientId) {
        int bucket = Math.abs(clientId.hashCode() %
                OutgoingMessageFlowLocalXodusPersistence.BUCKET_COUNT %
                this.bucketCount);
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
                getLocalPersistence(clientId).remove(clientId, messageId));
    }

    public ListenableFuture<Void> removeForClient(String clientId) {
        try {
            if (exists(clientId)) {
                return Futures.immediateFuture(null);
            }
            ListenableScheduledFuture scheduledFuture = this.clientScheduledFutures.get(clientId);
            if (scheduledFuture != null) {
                try {
                    scheduledFuture.cancel(true);
                } catch (CancellationException e) {
                }
            }
            return getPersistenceExecutor(clientId).add(() -> {
                removeAll(clientId);
                vectorClocks.getAndIncrement(clientId, clusterConnection.getClusterId());
                localMemoryPersistence.removeAll(clientId);
                localPersistence.removeAll(clientId);
                return null;
            });
        } catch (Throwable e) {
            return Futures.immediateFailedFuture(e);
        }
    }

    private boolean exists(String clientId) {
        Set<String> replicaNodes = this.clusterConnection.getReplicaNodes(clientId, this.replicateCount);
        String clusterId = this.clusterConnection.getClusterId();
        if (replicaNodes.contains(clusterId)) {
            return true;
        }
        String node = this.primaryRing.getNode(clientId);
        return clusterId.equals(node);
    }

    private void i(String clientId) {
        if (this.clientScheduledFutures.containsKey(clientId)) {
            return;
        }
        ListenableScheduledFuture scheduledFuture =
                this.clusterScheduledExecutorService.schedule(
                        new b(clientId, this.primaryRing, this.clusterConnection, this, this.clusterScheduledExecutorService, this.clientSessionLookupTableProvider.get(), this.replicationInterval)
                        , this.replicationInterval, TimeUnit.MILLISECONDS);
        this.clientScheduledFutures.put(clientId, scheduledFuture);
        ClusterFutures.addCallback(scheduledFuture, new FutureCallback<Object>() {

            @Override
            public void onSuccess(@javax.annotation.Nullable Object result) {
                clientScheduledFutures.remove(clientId);
            }

            @Override
            public void onFailure(Throwable t) {
                if (!(t instanceof CancellationException)) {
                    LOGGER.error("Exception while sending outgoing message flow put request", t);
                }
                clientScheduledFutures.remove(clientId);
            }
        });
    }

    public ListenableFuture<Void> put(String clientId) {
        try {
            String node = this.primaryRing.getNode(clientId);
            OutgoingMessageFlowPutRequest request = new OutgoingMessageFlowPutRequest(clientId, getAll(clientId));
            ClusterRequestFuture<Void, OutgoingMessageFlowPutRequest> requestFuture = this.clusterConnection.send(request, node, Void.class);
            return requestFuture.setCallback(new OutgoingMessageFlowPutRequestCallback(this));
        } catch (Throwable e) {
            return Futures.immediateFailedFuture(e);
        }
    }

    public ListenableFuture<Void> handlePutRequest(@NotNull OutgoingMessageFlowPutRequest request, @NotNull String connectedNode) {
        try {
            SettableFuture<Void> settableFuture = SettableFuture.create();
            ListenableFuture<ClientSession> future = this.clientSessionLookupTableProvider.get().getLocally(request.getClientId());
            ClusterFutures.addCallback(future, new FutureCallback<ClientSession>() {

                @Override
                public void onSuccess(@javax.annotation.Nullable ClientSession result) {
                    if (result != null && result.getConnectedNode().equals(connectedNode)) {
                        settableFuture.setFuture(getPersistenceExecutor(request.getClientId()).add(() -> {
                            VectorClock vectorClock = vectorClocks.getAndIncrement(request.getClientId(), clusterConnection.getClusterId());
                            if (!request.getKey().equals(clusterConnection.getClusterId())) {
                                localPersistence.putAll(request.getClientId(), request.getMessages());
                            }
                            replicateClientFlow(request.getClientId(), request.getMessages(), vectorClock);
                            return null;
                        }));
                    }
                    settableFuture.set(null);
                }

                @Override
                public void onFailure(Throwable t) {
                    settableFuture.setException(t);
                }
            });
            return settableFuture;
        } catch (Throwable e) {
            return Futures.immediateFailedFuture(e);
        }
    }

    public ListenableFuture<Void> replicateClientFlow(@NotNull String clientId, @NotNull ImmutableList<MessageWithId> messages, @NotNull VectorClock requestVectorClock) {
        try {
            Preconditions.checkNotNull(clientId, "Client id must not be null");
            Preconditions.checkNotNull(messages, "Messages must not be null");
            Preconditions.checkNotNull(requestVectorClock, "Vector clock must not be null");
            OutgoingMessageFlowPutReplicateRequest request = new OutgoingMessageFlowPutReplicateRequest(clientId, messages, requestVectorClock);
            ImmutableList<ClusterRequestFuture<Void, OutgoingMessageFlowPutReplicateRequest>> replicateFutures = replicate(request);
            ImmutableList.Builder<ListenableFuture<Void>> builder = ImmutableList.builder();
            replicateFutures.forEach(replicateFuture -> {
                ListenableFuture<Void> future = replicateFuture.setCallback(new OutgoingMessageFlowPutReplicateRequestCallback(this));
                builder.add(future);
            });
            return ClusterFutures.merge(builder.build());
        } catch (Throwable e) {
            return Futures.immediateFailedFuture(e);
        }
    }

    private <Q extends ClusterKeyRequest> ImmutableList<ClusterRequestFuture<Void, Q>> replicate(@NotNull Q request) {
        Preconditions.checkNotNull(request, "Request must not be null");
        Set<String> replicaNodes = this.clusterConnection.getReplicaNodes(request.getKey(), this.replicateCount);
        ImmutableList.Builder<ClusterRequestFuture<Void, Q>> builder = ImmutableList.builder();
        replicaNodes.forEach(node -> {
            LOGGER.trace("Replicate outgoing message flow to {}", node);
            builder.add(this.clusterConnection.send(request, node, Void.class));
        });
        return builder.build();
    }

    public ListenableFuture<Void> handlePutReplica(OutgoingMessageFlowPutReplicateRequest request) {
        if (connected(request.getClientId())) {
            return Futures.immediateFuture(null);
        }
        return getPersistenceExecutor(request.getClientId()).add(() -> {
            vectorClocks.put(request.getClientId(), request.getVectorClock());
            localPersistence.putAll(request.getClientId(), request.getMessages());
            return null;
        });
    }

    public ListenableFuture<Void> add(String clientId, MessageWithId message) {
        return getPersistenceExecutor(clientId).add(() -> {
            localPersistence.addOrReplace(clientId, message.getMessageId(), message);
            VectorClock vectorClock = vectorClocks.getAndIncrement(clientId, clusterConnection.getClusterId());
            ImmutableList<MessageWithId> messages = localPersistence.drain(clientId);
            replicateClientFlow(clientId, messages, vectorClock);
            return null;
        });
    }

    public ListenableFuture<Void> removeAll(@NotNull String clientId) {
        try {
            Preconditions.checkNotNull(clientId, "Client id must not be null");
            return getPersistenceExecutor(clientId).add(() -> {
                put(clientId);
                VectorClock vectorClock = vectorClocks.getAndIncrement(clientId, clusterConnection.getClusterId());
                localPersistence.removeAll(clientId);
                localMemoryPersistence.removeAll(clientId);
                replicateRemoveAll(clientId, vectorClock);
                return null;
            });
        } catch (Throwable e) {
            return Futures.immediateFailedFuture(e);
        }
    }

    public ListenableFuture<Void> replicateRemoveAll(String clientId, VectorClock requestVectorClock) {
        try {
            OutgoingMessageFlowRemoveAllReplicateRequest request = new OutgoingMessageFlowRemoveAllReplicateRequest(clientId, requestVectorClock);
            ImmutableList<ClusterRequestFuture<Void, OutgoingMessageFlowRemoveAllReplicateRequest>> requestFutures = replicate(request);
            ImmutableList.Builder<ListenableFuture<Void>> builder = ImmutableList.builder();
            requestFutures.forEach(requestFuture -> {
                ListenableFuture<Void> future = requestFuture.setCallback(new OutgoingMessageFlowRemoveAllReplicateRequestCallback(this));
                builder.add(future);
            });
            return ClusterFutures.merge(builder.build());
        } catch (Throwable e) {
            return Futures.immediateFailedFuture(e);
        }
    }

    public ListenableFuture<Void> handleRemoveAllReplica(OutgoingMessageFlowRemoveAllReplicateRequest request) {
        if (connected(request.getClientId())) {
            return Futures.immediateFuture(null);
        }
        return getPersistenceExecutor(request.getClientId()).add(() -> {
            vectorClocks.put(request.getClientId(), request.getVectorClock());
            localPersistence.removeAll(request.getClientId());
            localMemoryPersistence.removeAll(request.getClientId());
            return null;
        });
    }

    public int size(String clientId) {
        return this.localPersistence.size(clientId);
    }

    public ImmutableList<MessageWithId> getAll(@NotNull String clientId) {
        return getLocalPersistence(clientId).drain(clientId);
    }

    public ListenableFuture<OutgoingMessageFlowReplicateRequest> getLocalData(@NotNull Filter filter) {
        Set<String> clients = this.localPersistence.getClients();
        List<ListenableFuture<OutgoingMessageFlowPutReplicateRequest>> futures =
                clients.stream()
                        .filter(filter::test)
                        .map(clientId ->
                                getPersistenceExecutor(clientId).add(() -> {
                                    VectorClock localVectorClock = vectorClocks.get(clientId);
                                    ImmutableList<MessageWithId> messages = localPersistence.drain(clientId);
                                    return new OutgoingMessageFlowPutReplicateRequest(clientId, messages, localVectorClock);
                                })
                        )
                        .collect(Collectors.toList());
        SettableFuture<OutgoingMessageFlowReplicateRequest> settableFuture = SettableFuture.create();
        ListenableFuture<List<OutgoingMessageFlowPutReplicateRequest>> future = Futures.allAsList(futures);
        ClusterFutures.addCallback(future, new FutureCallback<List<OutgoingMessageFlowPutReplicateRequest>>() {

            @Override
            public void onSuccess(@javax.annotation.Nullable List<OutgoingMessageFlowPutReplicateRequest> result) {
                if (result != null) {
                    settableFuture.set(new OutgoingMessageFlowReplicateRequest(ImmutableList.copyOf(result)));
                } else {
                    settableFuture.set(new OutgoingMessageFlowReplicateRequest(ImmutableList.of()));
                }
            }

            @Override
            public void onFailure(Throwable t) {
                settableFuture.setException(t);
            }
        });
        return settableFuture;
    }

    public ListenableFuture<Void> removeLocally(Filter filter) {
        Set<String> clients = this.localPersistence.getClients();
        List<ListenableFuture<Void>> futures = clients.stream()
                .filter(filter::test)
                .map(clientId ->
                        getPersistenceExecutor(clientId).add(() -> {
                            localPersistence.removeAll(clientId);
                            vectorClocks.remove(clientId);
                            return (Void) null;
                        }))
                .collect(Collectors.toList());
        return ClusterFutures.merge(futures);
    }

    public ListenableFuture<Void> processReplicationRequest(OutgoingMessageFlowReplicateRequest request) {
        List<ListenableFuture<Void>> futures = new ArrayList<>();
        request.getRequests().forEach(putReplicateRequest ->
                getPersistenceExecutor(putReplicateRequest.getClientId()).add(() -> {
                    String clientId = putReplicateRequest.getClientId();
                    VectorClock requestVectorClock = putReplicateRequest.getVectorClock();
                    VectorClock localVectorClock = vectorClocks.get(clientId);
                    if (requestVectorClock.before(localVectorClock) ||
                            requestVectorClock.equals(localVectorClock)) {
                        return null;
                    }
                    if (localVectorClock.before(requestVectorClock)) {
                        vectorClocks.put(clientId, requestVectorClock);
                        localPersistence.putAll(clientId, putReplicateRequest.getMessages());
                        return null;
                    }
                    LOGGER.trace("Conflict for outgoing message flow for client {}. Local vector clock={}, request vector clock={}.",
                            clientId, localVectorClock, requestVectorClock);
                    String node = primaryRing.getNode(clientId);
                    if (!node.equals(clusterConnection.getClusterId())) {
                        return null;
                    }
                    localVectorClock.merge(requestVectorClock);
                    localVectorClock.increment(clusterConnection.getClusterId());
                    vectorClocks.put(clientId, localVectorClock);
                    ImmutableList<MessageWithId> messages = localPersistence.drain(clientId);
                    Set<Integer> localHashSet = new HashSet<>();




                    Iterator localIterator1 = putReplicateRequest.getMessages().iterator();
                    MessageWithId localMessageWithID11;
                    while (localIterator1.hasNext()) {
                        localMessageWithID11 = (MessageWithId) localIterator1.next();
                        Iterator localIterator2 = messages.iterator();
                        while (localIterator2.hasNext()) {
                            MessageWithId localMessageWithID12 = (MessageWithId) localIterator2.next();
                            if (localMessageWithID11.getMessageId() == localMessageWithID12.getMessageId()) {
                                localHashSet.add(localMessageWithID11.getMessageId());
                                Object localObject1;
                                Object localObject2;
                                if (localMessageWithID12 instanceof InternalPublish &&
                                        localMessageWithID11 instanceof InternalPublish) {
                                    localObject1 = localMessageWithID12;
                                    localObject2 = localMessageWithID11;
                                    LOGGER.debug("Message id collision on message flow merge for publish {} and publish {} of client {}. Id was {}.",
                                            ((InternalPublish) localObject1).getUniqueId(), ((InternalPublish) localObject2).getUniqueId(), clientId, localMessageWithID12.getMessageId());

                                    publishDispatcherProvider.get().dispatch((InternalPublish) localObject1, clientId, ((InternalPublish) localObject1).getQoS().getQosNumber(), false, true, true);
                                    if (!((InternalPublish) localObject2).getUniqueId().equals(((InternalPublish) localObject1).getUniqueId())) {
                                        publishDispatcherProvider.get().dispatch((InternalPublish) localObject2, clientId, ((InternalPublish) localObject2).getQoS().getQosNumber(), false, false, true);
                                    }
                                } else if (localMessageWithID12 instanceof PubRel &&
                                        localMessageWithID11 instanceof InternalPublish) {
                                    localObject1 = (PubRel) localMessageWithID12;
                                    localObject2 = (InternalPublish) localMessageWithID11;
                                    LOGGER.debug("Message id collision on message flow merge for pubrel and publish {} of client {}. Id was {}.",
                                            ((InternalPublish) localObject2).getUniqueId(), clientId, localMessageWithID12.getMessageId());
                                    publishDispatcherProvider.get().dispatch((InternalPublish) localObject2, clientId, ((InternalPublish) localObject2).getQoS().getQosNumber(), false, false, true);
                                    publishDispatcherProvider.get().dispatch((PubRel) localObject1, clientId);
                                } else if (localMessageWithID12 instanceof InternalPublish &&
                                        localMessageWithID11 instanceof PubRel) {
                                    localObject1 = (PubRel) localMessageWithID11;
                                    localObject2 = (InternalPublish) localMessageWithID12;
                                    LOGGER.debug("Message id collision on message flow merge for pubrel and publish {} of client {}. Id was {}.",
                                            ((InternalPublish) localObject2).getUniqueId(), clientId, localMessageWithID12.getMessageId());
                                    publishDispatcherProvider.get().dispatch((InternalPublish) localObject2, clientId, ((InternalPublish) localObject2).getQoS().getQosNumber(), false, false, true);
                                    publishDispatcherProvider.get().dispatch((PubRel) localObject1, clientId);
                                } else if (localMessageWithID12 instanceof PubRel &&
                                        localMessageWithID11 instanceof PubRel) {
                                    localObject1 = (PubRel) localMessageWithID11;
                                    localObject2 = (PubRel) localMessageWithID11;
                                    LOGGER.debug("Message id collision on message flow merge for two pubrel messages of client {}. Id was {}.",
                                            clientId, localMessageWithID12.getMessageId());
                                    publishDispatcherProvider.get().dispatch((PubRel) localObject2, clientId);
                                }
                            }
                        }
                    }
                    localIterator1 = putReplicateRequest.getMessages().iterator();
                    while (localIterator1.hasNext()) {
                        localMessageWithID11 = (MessageWithId) localIterator1.next();
                        if (!localHashSet.contains(localMessageWithID11.getMessageId())) {
                            a(localMessageWithID11, clientId);
                        }
                    }
                    localIterator1 = messages.iterator();
                    while (localIterator1.hasNext()) {
                        localMessageWithID11 = (MessageWithId) localIterator1.next();
                        if (!localHashSet.contains(localMessageWithID11.getMessageId())) {
                            a(localMessageWithID11, clientId);
                        }
                    }
                    if (exists(clientId)) {
                        removeAll(clientId);
                    } else {
                        replicateRemoveAll(clientId, vectorClocks.getAndIncrement(clientId, clusterConnection.getClusterId()));
                    }
                    return null;
                })
        );
        return ClusterFutures.merge(futures);
    }

    private void a(MessageWithId message, String clientId) {
        if (message instanceof InternalPublish) {
            InternalPublish publish = (InternalPublish) message;
            this.publishDispatcherProvider.get().dispatch(publish, clientId, publish.getQoS().getQosNumber(), false, false, false);
            return;
        }
        if (message instanceof PubRel) {
            PubRel pubRel = (PubRel) message;
            this.publishDispatcherProvider.get().dispatch(pubRel, clientId);
            return;
        }
    }


    private boolean connected(String clientId) {
        return this.channelPersistence.getChannel(clientId) != null;
    }

    static class a
            extends ClusterResponseHandler<Void, OutgoingMessageFlowPutRequest> {
        private final String clientId;
        private final ListeningScheduledExecutorService clusterScheduledExecutorService;
        private final ConsistentHashingRing primaryRing;
        private final OutgoingMessageFlowSinglePersistence outgoingMessageFlowSinglePersistence;
        private final ClientSessionClusterPersistence clientSessionClusterPersistence;
        private final int replicationInterval;

        a(String clientId,
          ListeningScheduledExecutorService clusterScheduledExecutorService,
          ConsistentHashingRing primaryRing,
          OutgoingMessageFlowSinglePersistence outgoingMessageFlowSinglePersistence,
          ClientSessionClusterPersistence clientSessionClusterPersistence,
          int replicationInterval) {
            this.clientId = clientId;
            this.clusterScheduledExecutorService = clusterScheduledExecutorService;
            this.primaryRing = primaryRing;
            this.outgoingMessageFlowSinglePersistence = outgoingMessageFlowSinglePersistence;
            this.clientSessionClusterPersistence = clientSessionClusterPersistence;
            this.replicationInterval = replicationInterval;
        }

        public void a(@Nullable Void paramVoid) {
        }

        public void a() {
            this.clusterScheduledExecutorService.schedule(
                    new b(this.clientId, this.primaryRing, this.clusterConnection,
                            this.outgoingMessageFlowSinglePersistence, this.clusterScheduledExecutorService,
                            this.clientSessionClusterPersistence, this.replicationInterval),
                    this.replicationInterval, TimeUnit.MILLISECONDS);
        }

        public void b() {
            this.clusterScheduledExecutorService.schedule(
                    new b(this.clientId, this.primaryRing, this.clusterConnection,
                            this.outgoingMessageFlowSinglePersistence, this.clusterScheduledExecutorService,
                            this.clientSessionClusterPersistence, this.replicationInterval),
                    this.replicationInterval, TimeUnit.MILLISECONDS);
        }

        public void c() {
            this.clusterScheduledExecutorService.schedule(
                    new b(this.clientId, this.primaryRing, this.clusterConnection,
                            this.outgoingMessageFlowSinglePersistence, this.clusterScheduledExecutorService,
                            this.clientSessionClusterPersistence, this.replicationInterval),
                    this.replicationInterval, TimeUnit.MILLISECONDS);
        }

        public void a(Throwable t) {
            LOGGER.error("Exception while sending outgoing message flow put request.", t);
        }

        public void f() {
            LOGGER.warn("Outgoing message flow put request failed.");
        }

        public void d() {
            LOGGER.trace("Outgoing message flow put request timed out.");
            this.clusterScheduledExecutorService.schedule(
                    new b(this.clientId, this.primaryRing, this.clusterConnection,
                            this.outgoingMessageFlowSinglePersistence, this.clusterScheduledExecutorService,
                            this.clientSessionClusterPersistence, this.replicationInterval),
                    this.replicationInterval, TimeUnit.MILLISECONDS);
        }

        public void e() {
            a(Void.class);
        }
    }

    public static class b implements Runnable {
        private final String clientId;
        private final ConsistentHashingRing primaryRing;
        private final ClusterConnection clusterConnection;
        private final OutgoingMessageFlowSinglePersistence outgoingMessageFlowSinglePersistence;
        private final ListeningScheduledExecutorService clusterScheduledExecutorService;
        private final ClientSessionClusterPersistence clientSessionClusterPersistence;
        private final int replicationInterval;

        public b(String clientId,
                 ConsistentHashingRing primaryRing,
                 ClusterConnection clusterConnection,
                 OutgoingMessageFlowSinglePersistence outgoingMessageFlowSinglePersistence,
                 ListeningScheduledExecutorService clusterScheduledExecutorService,
                 ClientSessionClusterPersistence clientSessionClusterPersistence,
                 int replicationInterval) {
            this.clientId = clientId;
            this.primaryRing = primaryRing;
            this.clusterConnection = clusterConnection;
            this.outgoingMessageFlowSinglePersistence = outgoingMessageFlowSinglePersistence;
            this.clusterScheduledExecutorService = clusterScheduledExecutorService;
            this.clientSessionClusterPersistence = clientSessionClusterPersistence;
            this.replicationInterval = replicationInterval;
        }

        public void run() {
            ListenableFuture<ClientSession> future = this.clientSessionClusterPersistence.requestSession(this.clientId);
            ClusterFutures.addCallback(future, new FutureCallback<ClientSession>() {

                @Override
                public void onSuccess(@javax.annotation.Nullable ClientSession result) {
                    if (result != null && result.isPersistentSession()) {
                        String node = primaryRing.getNode(clientId);
                        OutgoingMessageFlowPutRequest request = new OutgoingMessageFlowPutRequest(clientId, outgoingMessageFlowSinglePersistence.getAll(clientId));
                        ClusterRequestFuture locale = clusterConnection.send(request, node, Void.class);
                        locale.setCallback(new a(clientId, clusterScheduledExecutorService, primaryRing,
                                outgoingMessageFlowSinglePersistence, clientSessionClusterPersistence,
                                replicationInterval));
                    }
                }

                @Override
                public void onFailure(Throwable t) {
                    LOGGER.error("Unable to replicate outgoing message flow for client {}.", clientId, t);
                }
            });
        }
    }
}
