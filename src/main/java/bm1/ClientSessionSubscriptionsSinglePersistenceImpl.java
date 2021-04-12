package bm1;

import aj.ClusterFutures;
import ak.VectorClock;
import bc1.ClientSessionSubscriptionsLocalPersistence;
import bx.SharedTopicUtils;
import by.TopicTreeSinglePersistence;
import co.SharedSubscriptionServiceImpl;
import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.hivemq.spi.annotations.NotNull;
import com.hivemq.spi.annotations.Nullable;
import com.hivemq.spi.message.Topic;
import d.CacheScoped;
import u.Filter;
import u.NotImplementedException;
import u.PersistenceExecutor;
import w1.ClusterPublishRequest;
import y.ClientSessionSubscriptionsClusterPersistence;
import y.ClientSubscriptions;
import z1.ClientSessionSubscriptionReplicateRequest;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Iterator;
// TODO:
@CacheScoped
public class ClientSessionSubscriptionsSinglePersistenceImpl
        implements ClientSessionSubscriptionsSinglePersistence, ClientSessionSubscriptionsClusterPersistence {
    private final ClientSessionSubscriptionsLocalPersistence clientSessionSubscriptionsLocalPersistence;
    private final TopicTreeSinglePersistence topicTreeSinglePersistence;
    private final PersistenceExecutor persistenceExecutor;
    private final SharedSubscriptionServiceImpl sharedSubscriptionService;
    private final MetricRegistry metricRegistry;

    @Inject
    public ClientSessionSubscriptionsSinglePersistenceImpl(ClientSessionSubscriptionsLocalPersistence clientSessionSubscriptionsLocalPersistence,
                                                           TopicTreeSinglePersistence topicTreeSinglePersistence,
                                                           SharedSubscriptionServiceImpl sharedSubscriptionService,
                                                           MetricRegistry metricRegistry) {
        this.clientSessionSubscriptionsLocalPersistence = clientSessionSubscriptionsLocalPersistence;
        this.topicTreeSinglePersistence = topicTreeSinglePersistence;
        this.sharedSubscriptionService = sharedSubscriptionService;
        this.metricRegistry = metricRegistry;
        this.persistenceExecutor = new PersistenceExecutor("client-session-subscription-writer", metricRegistry);
    }

    public ListenableFuture<Void> addSubscription(@NotNull String clientId, @NotNull Topic topic) {
        SharedSubscriptionServiceImpl.SharedTopic sharedTopic = this.sharedSubscriptionService.getSharedTopic(topic.getTopic());
        ListenableFuture<Void> future;
        if (sharedTopic == null) {
            future = this.topicTreeSinglePersistence.addSubscription(
                    clientId, topic, SharedTopicUtils.getShared(false), null);
        } else {
            future = this.topicTreeSinglePersistence.addSubscription(
                    clientId, new Topic(sharedTopic.getTopic(), topic.getQoS()), SharedTopicUtils.getShared(true),
                    sharedTopic.getGroupId());
        }
        SettableFuture<Void> settableFuture = SettableFuture.create();
        ClusterFutures.addCallback(future, new FutureCallback<Void>() {

            @Override
            public void onSuccess(@javax.annotation.Nullable Void result) {
                ListenableFuture<Void> f = persistenceExecutor.add(() -> {
                    clientSessionSubscriptionsLocalPersistence.addSubscription(clientId, topic, System.currentTimeMillis());
                    return null;
                });
                settableFuture.setFuture(f);
            }

            @Override
            public void onFailure(Throwable t) {
                settableFuture.setException(t);
            }
        });
        return settableFuture;
    }

    public ListenableFuture<ClientSubscriptions> getSubscriptions(@NotNull String clientId) {
        return Futures.immediateFuture(new ClientSubscriptions(this.clientSessionSubscriptionsLocalPersistence.getSubscriptions(clientId)));
    }

    public ListenableFuture<Void> removeAllLocally(@NotNull String clientId) {
        ImmutableSet localImmutableSet = this.clientSessionSubscriptionsLocalPersistence.getSubscriptions(clientId);
        ArrayList localArrayList = Lists.newArrayList();
        Object localObject = localImmutableSet.iterator();
        while (((Iterator) localObject).hasNext()) {
            Topic localTopic = (Topic) ((Iterator) localObject).next();
            localArrayList.add(this.topicTreeSinglePersistence.removeSubscription(clientId, localTopic.getTopic()));
        }
        SettableFuture<Void> settableFuture = SettableFuture.create();
        ClusterFutures.addCallback(ClusterFutures.merge(localArrayList), new FutureCallback<Void>() {

            @Override
            public void onSuccess(@javax.annotation.Nullable Void result) {
                ListenableFuture<Void> f = persistenceExecutor.add(() -> {
                    clientSessionSubscriptionsLocalPersistence.removeAllSubscriptions(clientId, System.currentTimeMillis());
                    return null;
                });
                settableFuture.setFuture(f);
            }

            @Override
            public void onFailure(Throwable t) {
                settableFuture.setException(t);
            }
        });
        return settableFuture;
    }

    public ListenableFuture<Void> removeSubscription(@NotNull String clientId, @NotNull Topic topic) {
        ClusterPublishRequest locala = this.sharedSubscriptionService.getSharedTopic(topic.getTopic());
        ListenableFuture localListenableFuture;
        if (locala == null) {
            localListenableFuture = this.topicTreeSinglePersistence.removeSubscription(clientId, topic.getTopic());
        } else {
            localListenableFuture = this.topicTreeSinglePersistence.removeSubscription(clientId, locala.a());
        }
        SettableFuture<Void> settableFuture = SettableFuture.create();
        ClusterFutures.addCallback(localListenableFuture, new FutureCallback<Void>() {

            @Override
            public void onSuccess(@javax.annotation.Nullable Void result) {
                ListenableFuture<Void> f = persistenceExecutor.add(() -> {
                    clientSessionSubscriptionsLocalPersistence.removeSubscription(clientId, topic, System.currentTimeMillis());
                    return null;
                });
                settableFuture.setFuture(f);
            }

            @Override
            public void onFailure(Throwable t) {
                settableFuture.setException(t);
            }
        });
        return settableFuture;
    }

    public ListenableFuture<Void> cleanUp(long tombstoneMaxAge) {
        return this.persistenceExecutor.add(() -> {
            clientSessionSubscriptionsLocalPersistence.cleanUp(tombstoneMaxAge);
            return null;
        });
    }

    public ClientSubscriptions getClientSubscriptions(String clientId) {
        return new ClientSubscriptions(this.clientSessionSubscriptionsLocalPersistence.getSubscriptions(clientId));
    }

    public ListenableFuture<Void> replicateAdd(long requestTimestamp, @NotNull VectorClock requestVectorClock, @NotNull String clientId, @NotNull Topic topic) {
        throw new NotImplementedException("Cluster persistence method 'replicateAdd' not implemented in single instance persistence");
    }

    public ListenableFuture<Void> replicateRemove(long requestTimestamp, @NotNull VectorClock requestVectorClock, @NotNull String clientId, @NotNull Topic topic) {
        throw new NotImplementedException("Cluster persistence method 'replicateRemove' not implemented in single instance persistence");
    }

    public ListenableFuture<Void> replicateRemoveAll(long requestTimestamp, @NotNull VectorClock requestVectorClock, @NotNull String clientId) {
        throw new NotImplementedException("Cluster persistence method 'replicateRemoveAll' not implemented in single instance persistence");
    }

    public ListenableFuture<Void> add(@NotNull String clientId, @Nullable Topic topic, @NotNull VectorClock requestVectorClock, long requestTimestamp) {
        throw new NotImplementedException("Cluster persistence method 'add' not implemented in single instance persistence");
    }

    public ListenableFuture<Void> remove(@NotNull String clientId, @Nullable Topic topic, @NotNull VectorClock requestVectorClock, long requestTimestamp) {
        throw new NotImplementedException("Cluster persistence method 'remove' not implemented in single instance persistence");
    }

    public ListenableFuture<Void> removeAll(@NotNull String clientId, @NotNull VectorClock requestVectorClock, long requestTimestamp) {
        throw new NotImplementedException("Cluster persistence method 'removeAll' not implemented in single instance persistence");
    }

    public ListenableFuture<Void> handleReplica(@NotNull String clientId, @NotNull ImmutableSet<Topic> subscriptions, @NotNull VectorClock requestVectorClock, long requestTimestamp) {
        throw new NotImplementedException("Cluster persistence method 'handleReplica' not implemented in single instance persistence");
    }

    public ListenableFuture<ImmutableSet<ClientSessionSubscriptionReplicateRequest>> getDataForReplica(@NotNull Filter filter) {
        throw new NotImplementedException("Cluster persistence method 'getDataForReplica' not implemented in single instance persistence");
    }

    public ListenableFuture<Void> removeLocally(@NotNull Filter filter) {
        throw new NotImplementedException("Cluster persistence method 'removeLocally' not implemented in single instance persistence");
    }
}
