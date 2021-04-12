package bm1;

import aj.ClusterFutures;
import ak.VectorClock;
import bc1.ClientSessionSubscriptionsLocalPersistence;
import bx.SharedTopicUtils;
import by.TopicTreeSinglePersistence;
import co.SharedSubscriptionServiceImpl;
import co.SharedSubscriptionServiceImpl.SharedTopic;
import com.codahale.metrics.MetricRegistry;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.hivemq.spi.annotations.NotNull;
import com.hivemq.spi.annotations.Nullable;
import com.hivemq.spi.message.Topic;
import d.CacheScoped;
import i.ClusterConfigurationService;
import j1.ClusterRequestFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import q.ConsistentHashingRing;
import r1.ClientSessionSubscriptionAddReplicateRequestCallback;
import r1.ClientSessionSubscriptionAddRequestCallback;
import r1.ClientSessionSubscriptionGetRequestCallback;
import r1.ClientSessionSubscriptionRemoveAllReplicateRequestCallback;
import r1.ClientSessionSubscriptionRemoveReplicateRequestCallback;
import r1.ClientSessionSubscriptionRemoveRequestCallback;
import s.Primary;
import t.ClusterConnection;
import u.AbstractClusterPersistence;
import u.Filter;
import u.PersistenceExecutor;
import u.TimestampObject;
import u.VectorClocks;
import y.ClientSessionSubscriptionsClusterPersistence;
import y.ClientSubscriptions;
import z1.ClientSessionSubscriptionAddReplicateRequest;
import z1.ClientSessionSubscriptionAddRequest;
import z1.ClientSessionSubscriptionGetRequest;
import z1.ClientSessionSubscriptionRemoveAllReplicateRequest;
import z1.ClientSessionSubscriptionRemoveReplicateRequest;
import z1.ClientSessionSubscriptionRemoveRequest;
import z1.ClientSessionSubscriptionReplicateRequest;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

// TODO:
@CacheScoped
public class ClientSessionSubscriptionsClusterPersistenceImpl
        extends AbstractClusterPersistence<ClientSubscriptions>
        implements ClientSessionSubscriptionsSinglePersistence, ClientSessionSubscriptionsClusterPersistence {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientSessionSubscriptionsClusterPersistenceImpl.class);
    private final int replicateCount;
    private final ClientSessionSubscriptionsLocalPersistence clientSessionSubscriptionsLocalPersistence;
    private final TopicTreeSinglePersistence topicTreeSinglePersistence;
    private final PersistenceExecutor persistenceExecutor;
    private final VectorClocks vectorClocks;
    private final SharedSubscriptionServiceImpl sharedSubscriptionService;
    private final MetricRegistry metricRegistry;

    @Inject
    ClientSessionSubscriptionsClusterPersistenceImpl(ClientSessionSubscriptionsLocalPersistence clientSessionSubscriptionsLocalPersistence,
                                                     TopicTreeSinglePersistence topicTreeSinglePersistence,
                                                     ClusterConnection clusterConnection,
                                                     @Primary ConsistentHashingRing primaryRing,
                                                     ClusterConfigurationService clusterConfigurationService,
                                                     SharedSubscriptionServiceImpl sharedSubscriptionService,
                                                     MetricRegistry metricRegistry) {
        super(primaryRing, clusterConnection, ClientSubscriptions.class);
        this.clientSessionSubscriptionsLocalPersistence = clientSessionSubscriptionsLocalPersistence;
        this.topicTreeSinglePersistence = topicTreeSinglePersistence;
        this.sharedSubscriptionService = sharedSubscriptionService;
        this.metricRegistry = metricRegistry;
        this.persistenceExecutor = new PersistenceExecutor("client-session-subscription-writer", metricRegistry);
        this.vectorClocks = new VectorClocks();
        this.replicateCount = clusterConfigurationService.getReplicates().getSubscriptions().getReplicateCount();
    }

    public ListenableFuture<ClientSubscriptions> getSubscriptions(@NotNull String clientId) {
        try {
            Preconditions.checkNotNull(clientId, "Client id must not be null");
            ClientSessionSubscriptionGetRequest request = new ClientSessionSubscriptionGetRequest(clientId);
            ClusterRequestFuture<ClientSubscriptions, ClientSessionSubscriptionGetRequest> requestFuture = send(request);
            return requestFuture.setCallback(new ClientSessionSubscriptionGetRequestCallback(this));
        } catch (Throwable e) {
            return Futures.immediateFailedFuture(e);
        }
    }

    public ListenableFuture<Void> addSubscription(@NotNull String clientId, @NotNull Topic topic) {
        try {
            Preconditions.checkNotNull(clientId, "Client id must not be null");
            Preconditions.checkNotNull(topic, "Topic must not be null");
            if (topic.getTopic().startsWith("$SYS")) {
                this.topicTreeSinglePersistence.addSubscription(clientId, topic, SharedTopicUtils.getShared(false), null);
            }
            return modifySubscription(clientId, topic, false);
        } catch (Throwable e) {
            return Futures.immediateFailedFuture(e);
        }
    }

    public ListenableFuture<Void> removeSubscription(@NotNull String clientId, @NotNull Topic topic) {
        try {
            Preconditions.checkNotNull(clientId, "Client id must not be null");
            Preconditions.checkNotNull(topic, "Topic must not be null");
            if (topic.getTopic().startsWith("$SYS")) {
                this.topicTreeSinglePersistence.removeSubscription(clientId, topic.getTopic());
            }
            return modifySubscription(clientId, topic, true);
        } catch (Throwable localThrowable) {
            return Futures.immediateFailedFuture(localThrowable);
        }
    }

    public ListenableFuture<Void> removeAllLocally(@NotNull String clientId) {
        try {
            Preconditions.checkNotNull(clientId, "Client id must not be null");
            String clusterId = clusterConnection.getClusterId();
            long l = System.currentTimeMillis();
            ImmutableSet localImmutableSet = this.clientSessionSubscriptionsLocalPersistence.getSubscriptions(clientId);
            Object localObject1 = localImmutableSet.iterator();
            while (((Iterator) localObject1).hasNext()) {
                localObject2 = (Topic) ((Iterator) localObject1).next();
                this.topicTreeSinglePersistence.removeSubscription(clientId, ((Topic) localObject2).getTopic());
            }
            ListenableFuture<VectorClock> future = this.persistenceExecutor.add(() -> {
                clientSessionSubscriptionsLocalPersistence.removeAllSubscriptions(clientId, l);
                return vectorClocks.getAndIncrement(clientId, clusterId);
            });
            SettableFuture<Void> settableFuture = SettableFuture.create();
            ClusterFutures.addCallback(future, new FutureCallback<VectorClock>() {

                @Override
                public void onSuccess(@javax.annotation.Nullable VectorClock result) {
                    ListenableFuture<Void> f = replicateRemoveAll(l, result, clientId);
                    ClusterFutures.setFuture(f, settableFuture);
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


    private ListenableFuture<Void> modifySubscription(@NotNull String clientId, @NotNull Topic topic, boolean remove) {
        try {
            String originNode = originNode(clientId);
            String clusterId = clusterConnection.getClusterId();
            long timestamp = System.currentTimeMillis();
            if (!originNode.equals(clusterId)) {
                if (remove) {
                    this.LOGGER.trace("Send client session subscription REMOVE TOPIC to {}", originNode);
                    ClusterRequestFuture<Void, ClientSessionSubscriptionRemoveRequest> requestFuture = clusterConnection.send(
                            new ClientSessionSubscriptionRemoveRequest(timestamp, clientId, topic), originNode, Void.class);
                    return requestFuture.setCallback(new ClientSessionSubscriptionRemoveRequestCallback(this));
                }
                this.LOGGER.trace("Send client session subscription ADD to {}", originNode);
                ClusterRequestFuture<Void, ClientSessionSubscriptionAddRequest> requestFuture = clusterConnection.send(
                        new ClientSessionSubscriptionAddRequest(timestamp, clientId, topic), originNode, Void.class);
                return requestFuture.setCallback(new ClientSessionSubscriptionAddRequestCallback(this));
            }
            ListenableFuture<Void> topicMergeFuture;
            if (topic.getTopic().startsWith("$SYS")) {
                topicMergeFuture = Futures.immediateFuture(null);
            } else if (remove) {
                SharedTopic sharedTopic = this.sharedSubscriptionService.getSharedTopic(topic.getTopic());
                if (sharedTopic == null) {
                    topicMergeFuture = this.topicTreeSinglePersistence.removeSubscription(clientId, topic.getTopic());
                } else {
                    topicMergeFuture = this.topicTreeSinglePersistence.removeSubscription(clientId, sharedTopic.getTopic());
                }
            } else {
                SharedTopic sharedTopic = this.sharedSubscriptionService.getSharedTopic(topic.getTopic());
                if (sharedTopic == null) {
                    topicMergeFuture = this.topicTreeSinglePersistence.addSubscription(clientId, topic, SharedTopicUtils.getShared(false), null);
                } else {
                    topicMergeFuture = this.topicTreeSinglePersistence.addSubscription(clientId,
                            new Topic(sharedTopic.getTopic(), topic.getQoS()), SharedTopicUtils.getShared(true), sharedTopic.getGroupId());
                }
            }
            ListenableFuture<VectorClock> localSubscriptionsFuture = this.persistenceExecutor.add(() -> {
                if (remove) {
                    clientSessionSubscriptionsLocalPersistence.removeSubscription(clientId, topic, timestamp);
                } else {
                    clientSessionSubscriptionsLocalPersistence.addSubscription(clientId, topic, timestamp);
                }
                return vectorClocks.getAndIncrement(clientId, clusterId);
            });
            SettableFuture<Void> settableFuture = SettableFuture.create();
            ClusterFutures.addCallback(localSubscriptionsFuture, new FutureCallback<VectorClock>() {

                @Override
                public void onSuccess(@javax.annotation.Nullable VectorClock result) {
                    ListenableFuture<Void> replicateFuture;
                    if (remove) {
                        replicateFuture = replicateRemove(timestamp, result, clientId, topic);
                    } else {
                        replicateFuture = replicateAdd(timestamp, result, clientId, topic);
                    }
                    ClusterFutures.setFuture(replicateFuture, settableFuture);
                }

                @Override
                public void onFailure(Throwable t) {
                    settableFuture.setException(t);
                }
            });
            return ClusterFutures.merge(settableFuture, topicMergeFuture);
        } catch (Throwable e) {
            return Futures.immediateFailedFuture(e);
        }
    }

    public ListenableFuture<Void> replicateAdd(long requestTimestamp, VectorClock requestVectorClock, String clientId, Topic topic) {
        try {
            ClientSessionSubscriptionAddReplicateRequest request = new ClientSessionSubscriptionAddReplicateRequest(requestTimestamp, requestVectorClock, clientId, topic);
            ImmutableList<ClusterRequestFuture<Void, ClientSessionSubscriptionAddReplicateRequest>> requestFutures = replicate(request);
            List<ListenableFuture<Void>> futures = requestFutures.stream()
                    .map(requestFuture -> requestFuture.setCallback(new ClientSessionSubscriptionAddReplicateRequestCallback(this)))
                    .collect(Collectors.toList());
            return ClusterFutures.merge(futures);
        } catch (Throwable e) {
            return Futures.immediateFailedFuture(e);
        }
    }

    public ListenableFuture<Void> replicateRemove(long requestTimestamp, VectorClock requestVectorClock, String clientId, Topic topic) {
        try {
            ClientSessionSubscriptionRemoveReplicateRequest request = new ClientSessionSubscriptionRemoveReplicateRequest(requestTimestamp, requestVectorClock, clientId, topic);
            ImmutableList<ClusterRequestFuture<Void, ClientSessionSubscriptionRemoveReplicateRequest>> requestFutures = replicate(request);
            List<ListenableFuture<Void>> futures = requestFutures.stream()
                        .map(requestFuture->requestFuture.setCallback(new ClientSessionSubscriptionRemoveReplicateRequestCallback(this)))
                        .collect(Collectors.toList());
            return ClusterFutures.merge(futures);
        } catch (Throwable e) {
            return Futures.immediateFailedFuture(e);
        }
    }

    public ListenableFuture<Void> replicateRemoveAll(long requestTimestamp, VectorClock requestVectorClock, String clientId) {
        try {
            ClientSessionSubscriptionRemoveAllReplicateRequest request = new ClientSessionSubscriptionRemoveAllReplicateRequest(requestTimestamp, requestVectorClock, clientId);
            ImmutableList<ClusterRequestFuture<Void, ClientSessionSubscriptionRemoveAllReplicateRequest>> requestFutures = replicate(request);
            List<ListenableFuture<Void>> futures = requestFutures.stream()
                    .map(requestFuture->requestFuture.setCallback(new ClientSessionSubscriptionRemoveAllReplicateRequestCallback(this)))
                    .collect(Collectors.toList());
            return ClusterFutures.merge(futures);
        } catch (Throwable e) {
            return Futures.immediateFailedFuture(e);
        }
    }

    public ClientSubscriptions getClientSubscriptions(@NotNull String clientId) {
        return new ClientSubscriptions(this.clientSessionSubscriptionsLocalPersistence.getSubscriptions(clientId));
    }

    public ListenableFuture<Void> add(@NotNull String clientId, @Nullable Topic topic, @NotNull VectorClock requestVectorClock, long requestTimestamp) {
        try {
            Preconditions.checkNotNull(clientId, "Client id must not be null");
            Preconditions.checkNotNull(requestVectorClock, "Vector clock must not be null");
            return this.persistenceExecutor.add(() -> {
                a(clientId, topic, requestVectorClock, requestTimestamp, false);
                return null;
            });
        } catch (Throwable localThrowable) {
            return Futures.immediateFailedFuture(localThrowable);
        }
    }

    public ListenableFuture<Void> remove(@NotNull String clientId, @Nullable Topic topic, @NotNull VectorClock requestVectorClock, long requestTimestamp) {
        try {
            Preconditions.checkNotNull(clientId, "Client id must not be null");
            Preconditions.checkNotNull(requestVectorClock, "Vector clock must not be null");
            return this.persistenceExecutor.add(() -> {
                a(clientId, topic, requestVectorClock, requestTimestamp, true);
                return null;
            });
        } catch (Throwable localThrowable) {
            return Futures.immediateFailedFuture(localThrowable);
        }
    }

    public ListenableFuture<Void> removeAll(@NotNull String clientId, @NotNull VectorClock requestVectorClock, long requestTimestamp) {
        try {
            Preconditions.checkNotNull(clientId, "Client id must not be null");
            Preconditions.checkNotNull(requestVectorClock, "Vector clock must not be null");
            return this.persistenceExecutor.add(() -> {
                a(clientId, null, requestVectorClock, requestTimestamp, true);
                return null;
            });
        } catch (Throwable localThrowable) {
            return Futures.immediateFailedFuture(localThrowable);
        }
    }

    private void a(@NotNull String clientId, @Nullable Topic topic, @NotNull VectorClock requestVectorClock, long timestamp, boolean remove) {
        VectorClock localVectorClock = this.vectorClocks.get(clientId);
        if (requestVectorClock.before(localVectorClock) || requestVectorClock.equals(localVectorClock)) {
            return;
        }
        if (localVectorClock.before(requestVectorClock)) {
            this.vectorClocks.put(clientId, requestVectorClock);
            if (remove) {
                if (topic == null) {
                    this.clientSessionSubscriptionsLocalPersistence.removeAllSubscriptions(clientId, timestamp);
                } else {
                    this.clientSessionSubscriptionsLocalPersistence.removeSubscription(clientId, topic, timestamp);
                }
            } else {
                this.clientSessionSubscriptionsLocalPersistence.addSubscription(clientId, topic, timestamp);
            }
        } else {
            this.LOGGER.trace("Conflict for topic: {} of client: {}. Local vector clock={}, request vector clock={}.",
                    topic, clientId, localVectorClock, requestVectorClock);
            localVectorClock.merge(requestVectorClock);
            localVectorClock.increment(clusterConnection.getClusterId());
            this.vectorClocks.put(clientId, localVectorClock);
            Long localLong = this.clientSessionSubscriptionsLocalPersistence.getTimestamp(clientId);
            if ((localLong == null) || (a(localLong.longValue(), timestamp))) {
                if (remove) {
                    if (topic == null) {
                        this.clientSessionSubscriptionsLocalPersistence.removeAllSubscriptions(clientId, timestamp);
                    } else {
                        this.clientSessionSubscriptionsLocalPersistence.removeSubscription(clientId, topic, timestamp);
                    }
                } else {
                    this.clientSessionSubscriptionsLocalPersistence.addSubscription(clientId, topic, timestamp);
                }
            }
        }
    }

    public ListenableFuture<Void> handleReplica(@NotNull String clientId, @NotNull ImmutableSet<Topic> subscriptions, @NotNull VectorClock requestVectorClock, long requestTimestamp) {
        try {
            Preconditions.checkNotNull(clientId, "Client id must not be null");
            Preconditions.checkNotNull(requestVectorClock, "Vector clock must not be null");
            Preconditions.checkNotNull(subscriptions, "Subscriptions must not be null");
            return this.persistenceExecutor.add(() -> {
                a1(clientId, subscriptions, requestVectorClock, requestTimestamp);
                return null;
            });
        } catch (Throwable e) {
            return Futures.immediateFailedFuture(e);
        }
    }

    private void a1(String clientId, Set<Topic> subscriptions, VectorClock requestVectorClock, long paramLong) {
        VectorClock locala = this.vectorClocks.get(clientId);
        if ((requestVectorClock.before(locala)) || (requestVectorClock.equals(locala))) {
            return;
        }
        Object localObject1;
        Object localObject2;
        if (locala.before(requestVectorClock)) {
            this.vectorClocks.put(clientId, requestVectorClock);
            this.clientSessionSubscriptionsLocalPersistence.removeAllSubscriptions(clientId, paramLong);
            localObject1 = subscriptions.iterator();
            while (((Iterator) localObject1).hasNext()) {
                localObject2 = (Topic) ((Iterator) localObject1).next();
                this.clientSessionSubscriptionsLocalPersistence.addSubscription(clientId, (Topic) localObject2, paramLong);
            }
        } else {
            this.LOGGER.trace("Conflict for topic: {}. Local vector clock={}, request vector clock={}.", new Object[]{clientId, locala, requestVectorClock});
            locala.merge(requestVectorClock);
            locala.increment(clusterConnection.getClusterId());
            this.vectorClocks.put(clientId, locala);
            localObject1 = this.clientSessionSubscriptionsLocalPersistence.getSubscriptions(clientId);
            localObject2 = this.clientSessionSubscriptionsLocalPersistence.getTimestamp(clientId);
            Iterator localIterator;
            Topic localTopic;
            if (localObject2 == null) {
                this.clientSessionSubscriptionsLocalPersistence.removeAllSubscriptions(clientId, paramLong);
                localIterator = subscriptions.iterator();
                while (localIterator.hasNext()) {
                    localTopic = (Topic) localIterator.next();
                    this.clientSessionSubscriptionsLocalPersistence.addSubscription(clientId, localTopic, paramLong);
                }
            } else if (a(((Long) localObject2).longValue(), paramLong)) {
                this.clientSessionSubscriptionsLocalPersistence.removeAllSubscriptions(clientId, paramLong);
                localIterator = subscriptions.iterator();
                while (localIterator.hasNext()) {
                    localTopic = (Topic) localIterator.next();
                    this.clientSessionSubscriptionsLocalPersistence.addSubscription(clientId, localTopic, paramLong);
                }
            }
        }
    }

    public ListenableFuture<ImmutableSet<ClientSessionSubscriptionReplicateRequest>> getDataForReplica(@NotNull Filter filter) {
        try {
            Preconditions.checkNotNull(filter, "Filter must not be null");
            return this.persistenceExecutor.add(() -> {
                Map<String, TimestampObject<Set<Topic>>> subscriptions = clientSessionSubscriptionsLocalPersistence.getEntries(filter);
                ImmutableSet.Builder<ClientSessionSubscriptionReplicateRequest> localBuilder = ImmutableSet.builder();
                subscriptions.entrySet().stream()
                        .forEach(entry -> {
                            VectorClock localVectorClock = vectorClocks.get(entry.getKey());
                            ImmutableSet<Topic> topics = ImmutableSet.copyOf(entry.getValue().getObject());
                            ClientSessionSubscriptionReplicateRequest localh = new ClientSessionSubscriptionReplicateRequest(entry.getValue().getTimestamp(), localVectorClock, entry.getKey(), topics);
                            localBuilder.add(localh);
                        });
                return localBuilder.build();
            });
        } catch (Throwable e) {
            return Futures.immediateFailedFuture(e);
        }
    }

    public ListenableFuture<Void> removeLocally(@NotNull Filter filter) {
        Preconditions.checkNotNull(filter, "Filter must not be null");
        return this.persistenceExecutor.add(() -> {
            Set<String> clients = clientSessionSubscriptionsLocalPersistence.remove(filter);
            clients.forEach(vectorClocks::remove);
            return null;
        });
    }

    public ListenableFuture<Void> cleanUp(long tombstoneMaxAge) {
        return this.persistenceExecutor.add(() -> {
            Set<String> clientSessionSubscriptions = clientSessionSubscriptionsLocalPersistence.cleanUp(tombstoneMaxAge);
            clientSessionSubscriptions.forEach(vectorClocks::remove);
            return null;
        });
    }

    private boolean a(long paramLong1, long paramLong2) {
        return paramLong1 <= paramLong2;
    }

    protected String name() {
        return "client session subscription";
    }

    @Override
    protected int getReplicateCount() {
        return this.replicateCount;
    }

    @Override
    protected ClientSubscriptions get(String key) {
        return new ClientSubscriptions(this.clientSessionSubscriptionsLocalPersistence.getSubscriptions(key));
    }
}
