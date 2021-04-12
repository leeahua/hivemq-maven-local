package bm1;

import aj.ClusterFutures;
import ak.VectorClock;
import am1.Metrics;
import av.InternalConfigurationService;
import av.Internals;
import bc1.ClientSessionLocalPersistence;
import bh1.BucketUtils;
import bn1.OutgoingMessageFlowClusterPersistence;
import bn1.OutgoingMessageFlowSinglePersistence;
import bu.MessageIDPools;
import com.codahale.metrics.MetricRegistry;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.hivemq.spi.annotations.NotNull;
import d.CacheScoped;
import i.ClusterConfigurationService;
import j1.ClusterRequestFuture;
import k1.ClusterCallbackFactoryImpl;
import k1.DefaultClusterCallback;
import l1.ClientConnectedRequestCallback;
import l1.ClientDisconnectedRequestCallback;
import l1.ClientSessionGetRequestCallback;
import l1.ClientSessionReplicateRequestCallback;
import l1.NodeForPublishRequestCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import q.ConsistentHashingRing;
import s.Primary;
import t.ClusterConnection;
import t1.AllClientsRequest;
import t1.ClientConnectedRequest;
import t1.ClientDisconnectedRequest;
import t1.ClientSessionGetRequest;
import t1.ClientSessionReplicateRequest;
import t1.ClientTakeoverRequest;
import t1.NodeForPublishRequest;
import u.AbstractClusterPersistence;
import u.Filter;
import u.PersistenceExecutor;
import u.TimestampObject;
import u.VectorClocks;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

@CacheScoped
public class ClientSessionClusterPersistenceImpl extends AbstractClusterPersistence<ClientSession>
        implements ClientSessionSinglePersistence, ClientSessionClusterPersistence {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientSessionClusterPersistenceImpl.class);
    private final int replicateCount;
    private final ClientSessionLocalPersistence clientSessionLocalPersistence;
    private final QueuedMessagesClusterPersistence queuedMessagesClusterPersistence;
    private final ClientSessionSubscriptionsClusterPersistence clientSessionSubscriptionsClusterPersistence;
    private final OutgoingMessageFlowClusterPersistence outgoingMessageFlowClusterPersistence;
    private final OutgoingMessageFlowSinglePersistence outgoingMessageFlowSinglePersistence;
    private final InternalConfigurationService internalConfigurationService;
    private final Metrics metrics;
    private final MessageIDPools messageIDPools;
    private final VectorClocks vectorClocks;
    private final MetricRegistry metricRegistry;
    private List<PersistenceExecutor> persistenceExecutors;
    private int bucketCount;

    @Inject
    public ClientSessionClusterPersistenceImpl(ClientSessionLocalPersistence clientSessionLocalPersistence,
                                               ClusterConnection clusterConnection,
                                               @Primary ConsistentHashingRing primaryRing,
                                               ClusterConfigurationService clusterConfigurationService,
                                               QueuedMessagesClusterPersistence queuedMessagesClusterPersistence,
                                               ClientSessionSubscriptionsClusterPersistence clientSessionSubscriptionsClusterPersistence,
                                               OutgoingMessageFlowClusterPersistence outgoingMessageFlowClusterPersistence,
                                               OutgoingMessageFlowSinglePersistence outgoingMessageFlowSinglePersistence,
                                               InternalConfigurationService internalConfigurationService,
                                               Metrics metrics,
                                               MessageIDPools messageIDPools,
                                               MetricRegistry metricRegistry) {
        super(primaryRing, clusterConnection, ClientSession.class);
        this.clientSessionLocalPersistence = clientSessionLocalPersistence;
        this.queuedMessagesClusterPersistence = queuedMessagesClusterPersistence;
        this.clientSessionSubscriptionsClusterPersistence = clientSessionSubscriptionsClusterPersistence;
        this.outgoingMessageFlowClusterPersistence = outgoingMessageFlowClusterPersistence;
        this.outgoingMessageFlowSinglePersistence = outgoingMessageFlowSinglePersistence;
        this.internalConfigurationService = internalConfigurationService;
        this.metrics = metrics;
        this.messageIDPools = messageIDPools;
        this.metricRegistry = metricRegistry;
        this.replicateCount = clusterConfigurationService.getReplicates().getClientSession().getReplicateCount();
        this.vectorClocks = new VectorClocks();
    }

    @PostConstruct
    protected void init() {
        this.bucketCount = this.internalConfigurationService.getInt(Internals.PERSISTENCE_CLIENT_SESSIONS_BUCKET_COUNT);
        this.persistenceExecutors = new ArrayList<>();
        for (int bucket = 0; bucket < this.bucketCount; bucket++) {
            this.persistenceExecutors.add(
                    new PersistenceExecutor("client-session-writer-" + bucket, this.metricRegistry));
        }
    }

    public ListenableFuture<Boolean> isPersistent(@NotNull String clientId) {
        try {
            Preconditions.checkNotNull(clientId, "Client id must not be null");
            ListenableFuture<ClientSession> future = requestSession(clientId);
            SettableFuture<Boolean> settableFuture = SettableFuture.create();
            ClusterFutures.addCallback(future, new FutureCallback<ClientSession>() {

                @Override
                public void onSuccess(@Nullable ClientSession result) {
                    if (result == null) {
                        settableFuture.set(false);
                    } else if (!result.isPersistentSession() && !result.isConnected()) {
                        settableFuture.set(false);
                    } else {
                        settableFuture.set(true);
                    }
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

    public ListenableFuture<Void> disconnected(@NotNull String clientId) {
        try {
            Preconditions.checkNotNull(clientId, "Client id must not be null");
            return clientDisconnectedRequest(clientId, this.clusterConnection.getClusterId());
        } catch (Throwable e) {
            return Futures.immediateFailedFuture(e);
        }
    }

    public ListenableFuture<MessageFlow> persistent(@NotNull String clientId, boolean persistentSession) {
        try {
            Preconditions.checkNotNull(clientId, "Client id must not be null");
            return clientConnectedRequest(clientId, persistentSession, this.clusterConnection.getClusterId());
        } catch (Throwable e) {
            return Futures.immediateFailedFuture(e);
        }
    }

    public ListenableFuture<String> nodeForPublish(@NotNull String clientId, ExecutorService executorService) {
        try {
            Preconditions.checkNotNull(clientId, "Client id must not be null");
            String originNode = originNode(clientId);
            if (originNode.equals(this.clusterConnection.getClusterId())) {
                return getExecutor(clientId).add(() -> {
                    ClientSession clientSession = clientSessionLocalPersistence.get(clientId);
                    if (clientSession != null &&
                            clientSession.isConnected() &&
                            queuedMessagesClusterPersistence.isEmpty(clientId)) {
                        return clientSession.getConnectedNode();
                    }
                    return clusterConnection.getClusterId();
                });
            }
            NodeForPublishRequest request = new NodeForPublishRequest(clientId);
            ClusterRequestFuture<String, NodeForPublishRequest> requestFuture = this.clusterConnection.send(request, originNode, String.class, this.internalConfigurationService.getLong(Internals.CLUSTER_REQUEST_DEFAULT_TIMEOUT), executorService);
            return requestFuture.setCallback(new NodeForPublishRequestCallback(this), executorService);
        } catch (Throwable e) {
            return Futures.immediateFailedFuture(e);
        }
    }

    public ListenableFuture<Void> handleReplica(@NotNull String clientId, @NotNull ClientSession clientSession, long requestTimestamp, VectorClock requestVectorClock) {
        Preconditions.checkNotNull(clientId, "Client id must not be null");
        Preconditions.checkNotNull(clientSession, "Client session must not be null");
        return getExecutor(clientId).add(() -> {
            VectorClock localVectorClock = vectorClocks.get(clientId);
            if (requestVectorClock.before(localVectorClock) ||
                    requestVectorClock.equals(localVectorClock)) {
                return null;
            }
            if (localVectorClock.before(requestVectorClock)) {
                vectorClocks.put(clientId, requestVectorClock);
                clientSessionLocalPersistence.persistent(clientId, clientSession, requestTimestamp);
            } else {
                localVectorClock.merge(requestVectorClock);
                localVectorClock.increment(clusterConnection.getClusterId());
                vectorClocks.put(clientId, localVectorClock);
                ClientSession localClientSession = clientSessionLocalPersistence.get(clientId);
                if (!localClientSession.isConnected() && clientSession.isConnected()) {
                    clientSessionLocalPersistence.persistent(clientId, clientSession, requestTimestamp);
                }
            }
            return null;
        });
    }

    public ListenableFuture<Void> handleMergeReplica(@NotNull String clientId, @NotNull ClientSession clientSession, long timestamp) {
        Preconditions.checkNotNull(clientId, "Client id must not be null");
        Preconditions.checkNotNull(clientSession, "Client session must not be null");
        return getExecutor(clientId).add(() -> {
            ClientSession localClientSession = clientSessionLocalPersistence.get(clientId);
            if (primaryRing.getNode(clientId).equals(clusterConnection.getClusterId())) {
                boolean allConnected = localClientSession != null &&
                        localClientSession.isConnected() &&
                        clientSession.isConnected();
                if (allConnected) {
                    boolean differentNode = !localClientSession.getConnectedNode().endsWith(clientSession.getConnectedNode());
                    if (differentNode) {
                        takeoverForClientRequest(clientId, clientSession.getConnectedNode());
                        takeoverForClientRequest(clientId, localClientSession.getConnectedNode());
                    }
                }
            }
            clientSessionLocalPersistence.merge(clientId, clientSession, timestamp);
            return null;
        });
    }

    public ListenableFuture<String> handleNodeForPublishRequest(@NotNull String clientId) {
        Preconditions.checkNotNull(clientId, "Client id must not be null");
        return getExecutor(clientId).add(() -> {
            ClientSession clientSession = clientSessionLocalPersistence.get(clientId);
            if (clientSession != null &&
                    clientSession.isConnected() &&
                    queuedMessagesClusterPersistence.isEmpty(clientId)) {
                return clientSession.getConnectedNode();
            }
            return primaryRing.getNode(clientId);
        });
    }

    public ListenableFuture<MessageFlow> clientConnectedRequest(@NotNull String clientId, boolean persistentSession, @NotNull String connectedNode) {
        try {
            Preconditions.checkNotNull(clientId, "Client id must not be null");
            Preconditions.checkNotNull(connectedNode, "Connected nod id must not be null");
            String originNode = originNode(clientId);
            if (originNode.equals(this.clusterConnection.getClusterId())) {
                long timestamp = System.currentTimeMillis();
                ClientSession clientSession = new ClientSession(true, persistentSession, connectedNode);
                ListenableFuture<ClientSession> localClientSessionFuture = getExecutor(clientId).add(() ->
                        clientSessionLocalPersistence.get(clientId));
                SettableFuture<MessageFlow> settableFuture = SettableFuture.create();
                ClusterFutures.addCallback(localClientSessionFuture, new FutureCallback<ClientSession>() {

                    @Override
                    public void onSuccess(@Nullable ClientSession clientSessionResult) {
                        ListenableFuture<Void> takeoverFuture;
                        if (clientSessionResult != null &&
                                clientSessionResult.isConnected() &&
                                !clientSessionResult.getConnectedNode().equals(connectedNode)) {
                            takeoverFuture = takeoverForClientRequest(clientId, clientSessionResult.getConnectedNode());
                        } else {
                            takeoverFuture = null;
                        }
                        ListenableFuture<VectorClock> persistentFuture = getExecutor(clientId).add(() -> {
                            VectorClock vectorClock = vectorClocks.getAndIncrement(clientId, clusterConnection.getClusterId());
                            clientSessionLocalPersistence.persistent(clientId, clientSession, timestamp);
                            return vectorClock;
                        });
                        ClusterFutures.addCallback(persistentFuture, new FutureCallback<VectorClock>() {

                            @Override
                            public void onSuccess(@Nullable VectorClock vectorClockResult) {
                                clean(clientId, clientSession);
                                replicateClientSession(clientId, clientSession, timestamp, vectorClockResult);
                                if (takeoverFuture == null) {
                                    settableFuture.set(new MessageFlow(outgoingMessageFlowSinglePersistence.getAll(clientId)));
                                    return;
                                }
                                ClusterFutures.addCallback(takeoverFuture, new FutureCallback<Void>() {

                                    @Override
                                    public void onSuccess(@Nullable Void result) {
                                        settableFuture.set(new MessageFlow(outgoingMessageFlowSinglePersistence.getAll(clientId)));
                                    }

                                    @Override
                                    public void onFailure(Throwable t) {
                                        settableFuture.set(new MessageFlow(outgoingMessageFlowSinglePersistence.getAll(clientId)));
                                    }
                                });
                            }

                            @Override
                            public void onFailure(Throwable t) {
                                settableFuture.setException(t);
                            }
                        });
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        settableFuture.setException(t);
                    }
                });
                return settableFuture;
            }
            LOGGER.trace("Send client Connect to {}", originNode);
            ClientConnectedRequest request = new ClientConnectedRequest(persistentSession, clientId, connectedNode);
            ClusterRequestFuture<MessageFlow, ClientConnectedRequest> requestFuture = this.clusterConnection.send(request, originNode, MessageFlow.class);
            return requestFuture.setCallback(new ClientConnectedRequestCallback(this));
        } catch (Throwable e) {
            return Futures.immediateFailedFuture(e);
        }
    }


    private void clean(String clientId, ClientSession clientSession) {
        if (!clientSession.isPersistentSession() &&
                this.primaryRing.getNode(clientId).equals(this.clusterConnection.getClusterId())) {
            this.messageIDPools.remove(clientId);
            this.queuedMessagesClusterPersistence.remove(clientId);
            ClientSubscriptions subscriptions = this.clientSessionSubscriptionsClusterPersistence.getClientSubscriptions(clientId);
            this.metrics.subscriptionsCurrent().dec(subscriptions.getSubscriptions().size());
            this.clientSessionSubscriptionsClusterPersistence.removeAllLocally(clientId);
            this.outgoingMessageFlowClusterPersistence.removeAll(clientId);
        }
    }


    public ListenableFuture<Void> takeoverForClientRequest(String clientId, String node) {
        LOGGER.debug("Request client takeover for client {} at node {}.", clientId, node);
        ClientTakeoverRequest request = new ClientTakeoverRequest(clientId);
        ClusterRequestFuture<Void, ClientTakeoverRequest> future = this.clusterConnection.send(request, node, Void.class);
        return future.setCallback(new DefaultClusterCallback(Void.class));
    }

    public ListenableFuture<Void> clientDisconnectedRequest(@NotNull String clientId, @NotNull String connectedNode) {
        try {
            String originNode = originNode(clientId);
            if (originNode.equals(this.clusterConnection.getClusterId())) {
                long timestamp = System.currentTimeMillis();
                SettableFuture<Void> settableFuture = SettableFuture.create();
                getExecutor(clientId).add(() -> {
                    ClientSession clientSession = clientSessionLocalPersistence.disconnect(clientId, connectedNode, timestamp);
                    if (clientId != null &&
                            clientSession.getConnectedNode().equals(connectedNode)) {
                        VectorClock vectorClock = vectorClocks.getAndIncrement(clientId, clusterConnection.getClusterId());
                        clean(clientId, clientSession);
                        settableFuture.setFuture(replicateClientSession(clientId, clientSession, timestamp, vectorClock));
                        return null;
                    }
                    settableFuture.set(null);
                    return null;
                });
                return settableFuture;
            }
            LOGGER.trace("Send client Disconnect to {}", originNode);
            ClientDisconnectedRequest request = new ClientDisconnectedRequest(clientId, connectedNode);
            ClusterRequestFuture<Void, ClientDisconnectedRequest> locale = this.clusterConnection.send(request, originNode, Void.class);
            return locale.setCallback(new ClientDisconnectedRequestCallback(this));
        } catch (Throwable e) {
            return Futures.immediateFailedFuture(e);
        }
    }

    private void disconnectClient(String clientId, ClientSession clientSession) {
        this.queuedMessagesClusterPersistence.resetQueueDrained(clientId);
        if (this.primaryRing.getNode(clientId).equals(this.clusterConnection.getClusterId()) &&
                !clientSession.isPersistentSession()) {
            this.messageIDPools.remove(clientId);
            this.queuedMessagesClusterPersistence.remove(clientId);
            ClientSubscriptions subscriptions = this.clientSessionSubscriptionsClusterPersistence.getClientSubscriptions(clientId);
            this.metrics.subscriptionsCurrent().dec(subscriptions.getSubscriptions().size());
            this.clientSessionSubscriptionsClusterPersistence.removeAllLocally(clientId);
        }
    }

    public ListenableFuture<Void> disconnectAllClients(String node) {
        SettableFuture<Void> settableFuture = SettableFuture.create();
        List<ListenableFuture<ImmutableMap<String, ClientSession>>> futures = new ArrayList<>();
        for (int bucket = 0; bucket < this.bucketCount; bucket++) {
            int bucketIndex = bucket;
            futures.add(this.persistenceExecutors.get(bucket)
                    .add(() -> clientSessionLocalPersistence.disconnectAll(node, bucketIndex)));
        }
        ClusterFutures.addCallback(Futures.allAsList(futures), new FutureCallback<List<ImmutableMap<String, ClientSession>>>() {

            @Override
            public void onSuccess(@Nullable List<ImmutableMap<String, ClientSession>> result) {
                if (result == null) {
                    settableFuture.set(null);
                    return;
                }
                result.stream()
                        .flatMap(clientSessions -> clientSessions.entrySet().stream())
                        .forEach(entry -> disconnectClient(entry.getKey(), entry.getValue()));
                settableFuture.set(null);
            }

            @Override
            public void onFailure(Throwable t) {
                settableFuture.setException(t);
            }
        });
        return settableFuture;
    }

    public ListenableFuture<Void> replicateClientSession(String paramString, ClientSession clientSession, long requestTimestamp, VectorClock requestVectorClock) {
        try {
            ClientSessionReplicateRequest request = new ClientSessionReplicateRequest(requestTimestamp, requestVectorClock, paramString, clientSession);
            ImmutableList<ClusterRequestFuture<Void, ClientSessionReplicateRequest>> requestFutures = replicate(request);
            List<ListenableFuture<Void>> futures = requestFutures.stream()
                    .map(requestFuture -> requestFuture.setCallback(new ClientSessionReplicateRequestCallback(this)))
                    .collect(Collectors.toList());
            return ClusterFutures.merge(futures);
        } catch (Throwable e) {
            return Futures.immediateFailedFuture(e);
        }
    }

    public ListenableFuture<ImmutableSet<ClientSessionReplicateRequest>> getDataForReplica(Filter filter) {
        List<ListenableFuture<Map<String, TimestampObject<ClientSession>>>> futures = new ArrayList<>();
        for (int bucket = 0; bucket < this.bucketCount; bucket++) {
            int bucketIndex = bucket;
            futures.add((this.persistenceExecutors.get(bucket))
                    .add(() -> clientSessionLocalPersistence.get(filter, bucketIndex)));
        }
        SettableFuture<ImmutableSet<ClientSessionReplicateRequest>> settableFuture = SettableFuture.create();
        ClusterFutures.addCallback(Futures.allAsList(futures), new FutureCallback<List<Map<String, TimestampObject<ClientSession>>>>() {

            @Override
            public void onSuccess(@Nullable List<Map<String, TimestampObject<ClientSession>>> result) {
                if (result == null) {
                    settableFuture.set(ImmutableSet.of());
                    return;
                }
                Set<ClientSessionReplicateRequest> requests =
                        result.stream()
                                .flatMap(clientSessions -> clientSessions.entrySet().stream())
                                .map(entry -> new ClientSessionReplicateRequest(
                                        entry.getValue().getTimestamp(), null, entry.getKey(), entry.getValue().getObject()))
                                .collect(Collectors.toSet());
                settableFuture.set(ImmutableSet.copyOf(requests));
            }

            @Override
            public void onFailure(Throwable t) {
                settableFuture.setException(t);
            }
        });
        return settableFuture;
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

    public ListenableFuture<Set<String>> getLocalAllClients() {
        List<ListenableFuture<Map<String, TimestampObject<ClientSession>>>> futures = new ArrayList();
        for (int bucket = 0; bucket < this.bucketCount; bucket++) {
            int bucketIndex = bucket;
            futures.add(this.persistenceExecutors.get(bucket).add(() ->
                    clientSessionLocalPersistence.get(clientId -> true, bucketIndex)));
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

    public ListenableFuture<Set<String>> getAllClients() {
        ListenableFuture<Set<String>> localClientsFuture = getLocalAllClients();
        SettableFuture<Set<String>> settableFuture = SettableFuture.create();
        Set<String> clients = new HashSet();
        SettableFuture<Void> localSettableFuture = SettableFuture.create();
        ClusterFutures.addCallback(localClientsFuture, new FutureCallback<Set<String>>() {

            @Override
            public void onSuccess(@Nullable Set<String> result) {
                clients.addAll(result);
                localSettableFuture.set(null);
            }

            @Override
            public void onFailure(Throwable t) {
                settableFuture.setException(t);
            }
        });

        AllClientsRequest request = new AllClientsRequest();
        ListenableFuture<Void> clusterFuture = this.clusterConnection.send(
                request, Set.class, new ClusterCallbackFactoryImpl(Set.class), true,
                (node, result) -> clients.addAll(result));
        ListenableFuture<Void> future = ClusterFutures.merge(localSettableFuture, clusterFuture);
        ClusterFutures.addCallback(future, new FutureCallback<Void>() {

            @Override
            public void onSuccess(@Nullable Void result) {
                settableFuture.set(clients);
            }

            @Override
            public void onFailure(Throwable t) {
                settableFuture.setException(t);
            }
        });
        return settableFuture;
    }

    public ListenableFuture<ClientSession> requestSession(@NotNull String clientId) {
        try {
            Preconditions.checkNotNull(clientId, "Client id must not be null");
            ClientSessionGetRequest request = new ClientSessionGetRequest(clientId);
            ClusterRequestFuture<ClientSession, ClientSessionGetRequest> future = send(request);
            return future.setCallback(new ClientSessionGetRequestCallback(this));
        } catch (Throwable e) {
            return Futures.immediateFailedFuture(e);
        }
    }

    public ListenableFuture<ClientSession> getLocally(@NotNull String clientId) {
        Preconditions.checkNotNull(clientId, "Client id must not be null");
        return getExecutor(clientId).add(() -> clientSessionLocalPersistence.get(clientId));
    }

    public ListenableFuture<Void> removeLocally(Filter filter) {
        List<ListenableFuture<Void>> futures = new ArrayList();
        for (int bucket = 0; bucket < this.bucketCount; bucket++) {
            int bucketIndex = bucket;
            futures.add(this.persistenceExecutors.get(bucket).add(() -> {
                ImmutableSet<String> clients = clientSessionLocalPersistence.remove(filter, bucketIndex);
                clients.forEach(vectorClocks::remove);
                return null;
            }));
        }
        return ClusterFutures.merge(futures);
    }

    public ListenableFuture<Void> cleanUp(long tombstoneMaxAge) {
        List<ListenableFuture<Void>> futures = new ArrayList();
        for (int bucket = 0; bucket < this.bucketCount; bucket++) {
            int bucketIndex = bucket;
            futures.add(this.persistenceExecutors.get(bucket).add(() -> {
                Set<String> clientSessions = clientSessionLocalPersistence.cleanUp(tombstoneMaxAge, bucketIndex);
                clientSessions.forEach(vectorClocks::remove);
                return null;
            }));
        }
        return ClusterFutures.merge(futures);
    }

    @Override
    protected String name() {
        return "client session";
    }

    @Override
    protected int getReplicateCount() {
        return this.replicateCount;
    }

    @Override
    protected ClientSession get(@NotNull String key) {
        Preconditions.checkNotNull(key, "Key must not be null");
        return this.clientSessionLocalPersistence.get(key);
    }

    private PersistenceExecutor getExecutor(String clientId) {
        return this.persistenceExecutors.get(BucketUtils.bucket(clientId, this.bucketCount));
    }
}
