package ai;

import aa.TopicTreeAddRequest;
import aa.TopicTreeGetRequest;
import aa.TopicTreeRemoveRequest;
import aa.TopicTreeReplicateAddRequest;
import aa.TopicTreeRemoveReplicateRequest;
import aj.ClusterFutures;
import bx.SubscriberWithQoS;
import by.Segment;
import by.TopicTree;
import by.TopicTreeClusterPersistence;
import by.TopicTreeImpl;
import by.TopicTreeSinglePersistence;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.SettableFuture;
import com.hivemq.spi.annotations.NotNull;
import com.hivemq.spi.annotations.Nullable;
import com.hivemq.spi.message.Topic;
import d.CacheScoped;
import i.ClusterConfigurationService;
import j1.ClusterRequestFuture;
import j1.ClusterKeyRequest;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import q.ConsistentHashingRing;
import s.Cluster;
import s.Primary;
import s1.TopicTreeAddRequestCallback;
import s1.TopicTreeGetRequestCallback;
import s1.TopicTreeRemoveRequestCallback;
import s1.TopicTreeReplicateAddRequestCallback;
import s1.TopicTreeRemoveReplicateRequestCallback;
import t.ClusterConnection;
import u.AbstractClusterPersistence;
import u.Filter;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
// TODO:
@CacheScoped
public class TopicTreeClusterPersistenceImpl
        extends AbstractClusterPersistence<TopicSubscribers>
        implements TopicTreeClusterPersistence, TopicTreeSinglePersistence {
    public final int replicateCount;
    private static final Logger LOGGER = LoggerFactory.getLogger(TopicTreeClusterPersistenceImpl.class);
    private final TopicTree topicTree = new TopicTreeImpl(16, 16);
    private final TopicTree sysTopicTree = new TopicTreeImpl(16, 16);
    private final ClusterConnection clusterConnection;
    private final ListeningExecutorService clusterExecutor;

    @Inject
    public TopicTreeClusterPersistenceImpl(
            @Primary ConsistentHashingRing primaryRing,
            ClusterConnection clusterConnection,
            ClusterConfigurationService clusterConfigurationService,
            @Cluster ListeningExecutorService clusterExecutor) {
        super(primaryRing, clusterConnection, TopicSubscribers.class);
        this.clusterConnection = clusterConnection;
        this.clusterExecutor = clusterExecutor;
        this.replicateCount = clusterConfigurationService.getReplicates().getTopicTree().getReplicateCount();
    }

    public ListenableFuture<Void> addSubscription(@NotNull String subscriber, @NotNull Topic topic, byte shared, @Nullable String groupId) {
        try {
            Preconditions.checkNotNull(subscriber, "Subscriber must not be null");
            Preconditions.checkNotNull(topic, "Topic must not be null");
            if (topic.getTopic().startsWith("$SYS")) {
                this.sysTopicTree.addSubscription(subscriber, topic, shared, groupId);
                return Futures.immediateFuture(null);
            }
            String[] segmentKeys = StringUtils.splitPreserveAllTokens(topic.getTopic(), "/");
            String segmentKey = segmentKeys[0];
            return updateSubscription(subscriber, topic, segmentKey, false, shared, groupId);
        } catch (Throwable e) {
            return Futures.immediateFailedFuture(e);
        }
    }

    public void subscribe(String subscriber, Topic subscription, byte shared, String groupId) {
        if (subscription.getTopic().startsWith("$SYS")) {
            this.sysTopicTree.addSubscription(subscriber, subscription, shared, groupId);
        } else {
            this.topicTree.addSubscription(subscriber, subscription, shared, groupId);
        }
    }

    public ListenableFuture<Void> removeSubscription(@NotNull String subscriber, @NotNull String topic) {
        try {
            Preconditions.checkNotNull(subscriber, "Subscriber must not be null");
            Preconditions.checkNotNull(topic, "Topic must not be null");
            if (topic.startsWith("$SYS")) {
                this.sysTopicTree.removeSubscription(subscriber, topic);
                return Futures.immediateFuture(null);
            }
            String[] segmentKeys = StringUtils.splitPreserveAllTokens(topic, "/");
            String segmentKey = segmentKeys[0];
            return updateSubscription(subscriber, Topic.topicFromString(topic), segmentKey, true, (byte) 0, null);
        } catch (Throwable localThrowable) {
            return Futures.immediateFailedFuture(localThrowable);
        }
    }

    public ImmutableSet<SubscriberWithQoS> getLocalSubscribers(@NotNull String topic) {
        Preconditions.checkNotNull(topic, "Topic must not be null");
        if (topic.startsWith("$SYS")) {
            return this.sysTopicTree.getSubscribers(topic);
        }
        ImmutableSet.Builder localBuilder = ImmutableSet.builder();
        localBuilder.addAll(this.topicTree.getWildcardSubscribers());
        localBuilder.addAll(this.topicTree.getSubscribers(topic));
        return localBuilder.build();
    }

    private ListenableFuture<Void> updateSubscription(@NotNull String subscriber,
                                     @NotNull Topic topic,
                                     @NotNull String segmentKey,
                                     boolean remove,
                                     byte shared,
                                     @Nullable String groupId) {
        try {
            String originNode = originNode(segmentKey);
            String clusterId = this.clusterConnection.getClusterId();
            if (!originNode.equals(clusterId)) {
                if (remove) {
                    LOGGER.trace("Send topic tree REMOVE to {}", originNode);
                    TopicTreeRemoveRequest request = new TopicTreeRemoveRequest(segmentKey, topic.getTopic(), subscriber);
                    ClusterRequestFuture<Void, TopicTreeRemoveRequest> requestFuture = this.clusterConnection.send(request, originNode, Void.class);
                    return requestFuture.setCallback(new TopicTreeRemoveRequestCallback(this));
                }
                LOGGER.trace("Send topic tree ADD to {}", originNode);
                TopicTreeAddRequest request = new TopicTreeAddRequest(segmentKey, topic, subscriber, shared, groupId);
                ClusterRequestFuture<Void, TopicTreeAddRequest> requestFuture = this.clusterConnection.send(request, originNode, Void.class);
                return requestFuture.setCallback(new TopicTreeAddRequestCallback(this));
            }
            if (remove) {
                this.topicTree.removeSubscription(subscriber, topic.getTopic());
            } else {
                this.topicTree.addSubscription(subscriber, topic, shared, groupId);
            }
            if (remove) {
                return replicateRemove(subscriber, topic.getTopic(), segmentKey);
            }
            return replicateAdd(subscriber, topic, segmentKey, shared, groupId);
        } catch (Throwable e) {
            return Futures.immediateFailedFuture(e);
        }
    }

    public ListenableFuture<TopicSubscribers> getSubscribers(@NotNull String topic) {
        try {
            Preconditions.checkNotNull(topic, "Topic must not be null");
            if (topic.startsWith("$SYS")) {
                return Futures.immediateFuture(new TopicSubscribers(this.sysTopicTree.getSubscribers(topic)));
            }
            String[] segmentKeys = StringUtils.splitPreserveAllTokens(topic, "/");
            String segmentKey = segmentKeys[0];
            TopicTreeGetRequest request = new TopicTreeGetRequest(segmentKey, topic);
            ClusterRequestFuture<TopicSubscribers, TopicTreeGetRequest>  requestFuture = a(request);
            SettableFuture<TopicSubscribers> settableFuture = SettableFuture.create();
            ListenableFuture<TopicSubscribers> future = requestFuture.setCallback(new TopicTreeGetRequestCallback(this));
            Futures.addCallback(future, new FutureCallback<TopicSubscribers>() {

                @Override
                public void onSuccess(@javax.annotation.Nullable TopicSubscribers result) {
                    ImmutableSet.Builder<SubscriberWithQoS> builder = ImmutableSet.builder();
                    builder.addAll(topicTree.getWildcardSubscribers());
                    builder.addAll(result.getSubscribers());
                    settableFuture.set(new TopicSubscribers(builder.build()));
                }

                @Override
                public void onFailure(Throwable t) {
                    settableFuture.setException(t);
                }
            }, this.clusterExecutor);
            return settableFuture;
        } catch (Throwable e) {
            return Futures.immediateFailedFuture(e);
        }
    }

    protected ClusterRequestFuture<TopicSubscribers, TopicTreeGetRequest> a(@NotNull TopicTreeGetRequest request) {
        Preconditions.checkNotNull(request, "Request must not be null");
        Set localSet = persistNodes(request.getKey());
        if (localSet.contains(this.clusterConnection.getClusterId())) {
            TopicSubscribers localObject = f(request.getTopic());
            return new ClusterRequestFuture(Futures.immediateFuture(localObject), this.clusterConnection.getClusterId(), this.clusterConnection, request, 100L, null);
        }
        Object localObject = firstPersistNode(localSet);
        LOGGER.trace("Send {} GET to {}.", this.returnType.getSimpleName(), localObject);
        return this.clusterConnection.send(request, (String) localObject, this.returnType);
    }

    @Override
    protected TopicSubscribers get(String key) {
        return new TopicSubscribers(this.topicTree.getSubscribers(key));
    }

    @Override
    public ListenableFuture<Void> replicateAdd(@NotNull String subscriber, @NotNull Topic topic, @NotNull String segmentKey, byte shared, @Nullable String groupId) {
        try {
            List<ListenableFuture<Void>> futures = new ArrayList<>();
            TopicTreeReplicateAddRequest request = new TopicTreeReplicateAddRequest(topic, subscriber, segmentKey, shared, groupId);
            ImmutableList<ClusterRequestFuture<Void, TopicTreeReplicateAddRequest>> requestFutures;
            if (topic.getTopic().startsWith("#") || topic.getTopic().startsWith("+")) {
                requestFutures = c(request);
            } else {
                requestFutures = replicate(request);
            }
            requestFutures.forEach(requestFuture -> {
                ListenableFuture<Void> future = requestFuture.setCallback(new TopicTreeReplicateAddRequestCallback(this));
                futures.add(future);
            });
            return ClusterFutures.merge(futures);
        } catch (Throwable e) {
            return Futures.immediateFailedFuture(e);
        }
    }

    public ListenableFuture<Void> replicateRemove(@NotNull String subscriber, @NotNull String topic, @NotNull String segmentKey) {
        try {
            List<ListenableFuture<Void>> futures = new ArrayList<>();
            TopicTreeRemoveReplicateRequest request = new TopicTreeRemoveReplicateRequest(topic, subscriber, segmentKey);
            ImmutableList<ClusterRequestFuture<Void, TopicTreeRemoveReplicateRequest>> requestFutures;
            if (topic.startsWith("#") || topic.startsWith("+")) {
                requestFutures = c(request);
            } else {
                requestFutures = replicate(request);
            }
            requestFutures.forEach(requestFuture -> {
                ListenableFuture<Void> future = requestFuture.setCallback(new TopicTreeRemoveReplicateRequestCallback(this));
                futures.add(future);
            });
            return ClusterFutures.merge(futures);
        } catch (Throwable localThrowable) {
            return Futures.immediateFailedFuture(localThrowable);
        }
    }

    private <Q extends ClusterKeyRequest> ImmutableList<ClusterRequestFuture<Void, Q>> c(@NotNull Q request) {
        Preconditions.checkNotNull(request, "Request must not be null");
        Set<String> nodes = this.primaryRing.getNodes();
        ImmutableList.Builder<ClusterRequestFuture<Void, Q>> builder = ImmutableList.builder();
        nodes.forEach(node -> {
            LOGGER.trace("Replicate {} to {}", name(), node);
            builder.add(this.clusterConnection.send(request, node, Void.class));
        });
        return builder.build();
    }

    public void addTopicReplica(@NotNull String subscriber, @NotNull Topic topic, @NotNull String segmentKey, byte shared, @Nullable String groupId) {
        this.topicTree.addSubscription(subscriber, topic, shared, groupId);
    }

    public void removeTopicReplica(@NotNull String subscriber, @NotNull String topic, @NotNull String segmentKey) {
        this.topicTree.removeSubscription(subscriber, topic);
    }

    public void mergeSegment(@NotNull Segment segment, @NotNull String segmentKey) {
        Preconditions.checkNotNull(segment, "Segment must not be null");
        Preconditions.checkNotNull(segmentKey, "Segment key must not be null");
        this.topicTree.merge(segment, segmentKey);
    }

    public ImmutableSet<Segment> getLocalDate(@NotNull Filter filter) {
        Preconditions.checkNotNull(filter, "Filter must not be null");
        return this.topicTree.get(filter);
    }

    public void removeLocally(@NotNull Filter filter) {
        this.topicTree.remove(filter);
    }

    public Set<SubscriberWithQoS> getLocalSubscribersWithoutWildcard(@NotNull String topic) {
        Preconditions.checkNotNull(topic, "Topic must not be null");
        return this.topicTree.getSubscribers(topic);
    }

    public TopicSubscribers getLocally(@NotNull String segmentKey, @NotNull String topic) {
        Preconditions.checkNotNull(segmentKey, "Segment key must not be null");
        Preconditions.checkNotNull(topic, "Topic must not be null");
        ImmutableSet<SubscriberWithQoS> subscribers = this.topicTree.getSubscribers(topic, true);
        return new TopicSubscribers(subscribers);
    }

    protected String name() {
        return "topic tree";
    }

    protected int getReplicateCount() {
        return this.replicateCount;
    }
}
