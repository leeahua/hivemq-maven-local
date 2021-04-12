package x;

import aj.ClusterFutures;
import ak.VectorClock;
import av.InternalConfigurationService;
import av.Internals;
import bh1.BucketUtils;
import bz.RetainedMessage;
import com.codahale.metrics.MetricRegistry;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.hivemq.spi.annotations.NotNull;
import com.hivemq.spi.annotations.Nullable;
import com.hivemq.spi.topic.TopicMatcher;
import d.CacheScoped;
import i.ClusterConfigurationService;
import j1.ClusterReplicateRequest;
import j1.ClusterRequestFuture;
import org.eclipse.jetty.util.ConcurrentHashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import p1.RetainedMessageAddReplicateRequestCallback;
import p1.RetainedMessageAddRequestCallback;
import p1.RetainedMessageClearRequestCallback;
import p1.RetainedMessageGetRequestCallback;
import p1.RetainedMessageGetWithWildcardRequestCallback;
import p1.RetainedMessageRemoveReplicateRequestCallback;
import p1.RetainedMessageRemoveRequestCallback;
import q.ConsistentHashingRing;
import s.Primary;
import t.ClusterConnection;
import u.AbstractClusterPersistence;
import u.Filter;
import u.LocalMatchTopicFilter;
import u.PersistenceExecutor;
import u.TimestampObject;
import u.VectorClocks;
import x1.RetainedMessageAddReplicateRequest;
import x1.RetainedMessageAddRequest;
import x1.RetainedMessageClearRequest;
import x1.RetainedMessageGetRequest;
import x1.RetainedMessageRemoveReplicateRequest;
import x1.RetainedMessageRemoveRequest;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@CacheScoped
public class RetainedMessagesClusterPersistenceImpl
        extends AbstractClusterPersistence<RetainedMessage>
        implements RetainedMessagesClusterPersistence, RetainedMessagesSinglePersistence {
    private static final Logger LOGGER = LoggerFactory.getLogger(RetainedMessagesClusterPersistenceImpl.class);
    public final int replicateCount;
    private final RetainedMessagesLocalPersistence retainedMessagesLocalPersistence;
    private final List<PersistenceExecutor> persistenceExecutors;
    private final TopicMatcher topicMatcher;
    private final VectorClocks vectorClocks;
    private final InternalConfigurationService internalConfigurationService;
    private final MetricRegistry metricRegistry;
    private final AsyncFunction<Set, Set<String>> topicSetCovertFunction;
    private int bucketCount;

    @Inject
    RetainedMessagesClusterPersistenceImpl(
            @Primary ConsistentHashingRing primaryRing,
            ClusterConnection clusterConnection,
            RetainedMessagesLocalPersistence retainedMessagesLocalPersistence,
            TopicMatcher topicMatcher,
            ClusterConfigurationService clusterConfigurationService,
            InternalConfigurationService internalConfigurationService,
            MetricRegistry metricRegistry) {
        super(primaryRing, clusterConnection, RetainedMessage.class);
        this.retainedMessagesLocalPersistence = retainedMessagesLocalPersistence;
        this.topicMatcher = topicMatcher;
        this.internalConfigurationService = internalConfigurationService;
        this.metricRegistry = metricRegistry;
        this.vectorClocks = new VectorClocks();
        this.bucketCount = internalConfigurationService.getInt(Internals.PERSISTENCE_RETAINED_MESSAGES_BUCKET_COUNT);
        this.persistenceExecutors = new ArrayList<>(this.bucketCount);
        for (int bucket = 0; bucket < this.bucketCount; bucket++) {
            this.persistenceExecutors.add(new PersistenceExecutor("retained-message-writer-" + bucket, metricRegistry));
        }
        this.replicateCount = clusterConfigurationService.getReplicates().getRetainedMessage().getReplicateCount();
        this.topicSetCovertFunction = input -> {
            SettableFuture<Set<String>> settableFuture = SettableFuture.create();
            settableFuture.set(input);
            return settableFuture;
        };
    }

    public ListenableFuture<RetainedMessage> getWithoutWildcards(@NotNull String topic) {
        try {
            Preconditions.checkNotNull(topic, "Topic must not be null");
            if (topic.contains("+") || topic.contains("#")) {
                throw new IllegalArgumentException("Topic contains wildcard characters. Call getWithWildcards method instead.");
            }
            RetainedMessageGetRequest request = new RetainedMessageGetRequest(topic);
            ClusterRequestFuture<RetainedMessage, RetainedMessageGetRequest> future = send(request);
            return future.setCallback(new RetainedMessageGetRequestCallback(this));
        } catch (Throwable e) {
            return Futures.immediateFailedFuture(e);
        }
    }

    public long size() {
        return this.retainedMessagesLocalPersistence.size();
    }

    public ListenableFuture<Void> remove(@NotNull String topic) {
        try {
            Preconditions.checkNotNull(topic, "Topic must not be null");
            return update(topic, null, true);
        } catch (Throwable localThrowable) {
            return Futures.immediateFailedFuture(localThrowable);
        }
    }

    public ListenableFuture<Void> addOrReplace(@NotNull String topic, @NotNull RetainedMessage retainedMessage) {
        try {
            Preconditions.checkNotNull(topic, "Topic must not be null");
            Preconditions.checkNotNull(topic, "Retained message must not be null");
            return update(topic, retainedMessage, false);
        } catch (Throwable localThrowable) {
            return Futures.immediateFailedFuture(localThrowable);
        }
    }

    public ImmutableList<ListenableFuture<Set<String>>> getTopics(@NotNull String topic) {
        try {
            Preconditions.checkNotNull(topic, "Topic must not be null");
            ImmutableList.Builder<ListenableFuture<Set<String>>> builder = ImmutableList.builder();
            for (int bucket = 0; bucket < this.bucketCount; bucket++) {
                int bucketIndex = bucket;
                ListenableFuture<Set<String>> localFuture = this.persistenceExecutors.get(bucket).add(() ->
                        retainedMessagesLocalPersistence.getTopics(
                                new LocalMatchTopicFilter(topic, topicMatcher, primaryRing, clusterConnection.getClusterId()),
                                bucketIndex));
                builder.add(localFuture);
            }
            Set<String> nodes = this.primaryRing.getNodes();
            nodes.stream()
                    .filter(node -> !node.equals(this.clusterConnection.getClusterId()))
                    .forEach(node -> {
                        ClusterRequestFuture<Set, RetainedMessageGetRequest> requestFuture = this.clusterConnection.send(new RetainedMessageGetRequest(topic), node, Set.class);
                        ListenableFuture<Set> setFuture = requestFuture.setCallback(new RetainedMessageGetWithWildcardRequestCallback());
                        ListenableFuture<Set<String>> future = Futures.transformAsync(setFuture, this.topicSetCovertFunction);
                        builder.add(future);
                    });
            return builder.build();
        } catch (Throwable e) {
            return ImmutableList.of(Futures.immediateFailedFuture(e));
        }
    }

    private ListenableFuture<Void> update(@NotNull String topic, @Nullable RetainedMessage retainedMessage, boolean remove) {
        try {
            String originNode = originNode(topic);
            String clusterId = this.clusterConnection.getClusterId();
            long timestamp = System.currentTimeMillis();
            if (!originNode.equals(clusterId)) {
                if (remove) {
                    LOGGER.trace("Send retained message REMOVE to {}", originNode);
                    ClusterRequestFuture<Void, RetainedMessageRemoveRequest> requestFuture = this.clusterConnection.send(new RetainedMessageRemoveRequest(timestamp, topic), originNode, Void.class);
                    return requestFuture.setCallback(new RetainedMessageRemoveRequestCallback(this));
                }
                LOGGER.trace("Send retained message PUT to {}", originNode);
                ClusterRequestFuture<Void, RetainedMessageAddRequest> requestFuture = this.clusterConnection.send(new RetainedMessageAddRequest(timestamp, topic, retainedMessage), originNode, Void.class);
                return requestFuture.setCallback(new RetainedMessageAddRequestCallback(this));
            }
            ListenableFuture<VectorClock> localFuture = getExecutor(topic).add(() -> {
                if (remove) {
                    retainedMessagesLocalPersistence.remove(topic, timestamp);
                } else {
                    retainedMessagesLocalPersistence.addOrReplace(topic, retainedMessage, timestamp);
                }
                return vectorClocks.getAndIncrement(topic, clusterId);
            });
            SettableFuture<Void> settableFuture = SettableFuture.create();
            ClusterFutures.addCallback(localFuture, new FutureCallback<VectorClock>() {

                @Override
                public void onSuccess(@javax.annotation.Nullable VectorClock result) {
                    settableFuture.set(null);
                    if (remove) {
                        replicateRemove(timestamp, result, topic);
                    } else {
                        replicateAdd(timestamp, result, topic, retainedMessage);
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

    public RetainedMessage getLocally(@NotNull String topic) {
        return this.retainedMessagesLocalPersistence.get(topic);
    }

    public ListenableFuture<Void> replicateRemove(long requestTimestamp, VectorClock requestVectorClock, String topic) {
        try {
            List<ListenableFuture<Void>> futures = new ArrayList<>();
            RetainedMessageRemoveReplicateRequest request = new RetainedMessageRemoveReplicateRequest(requestTimestamp, requestVectorClock, topic);
            ImmutableList<ClusterRequestFuture<Void, RetainedMessageRemoveReplicateRequest>> requestFutures = replicate(request);
            requestFutures.forEach(requestFuture -> {
                ListenableFuture<Void> future = requestFuture.setCallback(new RetainedMessageRemoveReplicateRequestCallback(this));
                futures.add(future);
            });
            return ClusterFutures.merge(futures);
        } catch (Throwable e) {
            return Futures.immediateFailedFuture(e);
        }
    }

    public ListenableFuture<Void> replicateAdd(long requestTimestamp, VectorClock requestVectorClock, String topic, RetainedMessage retainedMessage) {
        try {
            List<ListenableFuture<Void>> futures = new ArrayList<>();
            RetainedMessageAddReplicateRequest request = new RetainedMessageAddReplicateRequest(requestTimestamp, requestVectorClock, topic, retainedMessage);
            ImmutableList<ClusterRequestFuture<Void, RetainedMessageAddReplicateRequest>> requestFutures = replicate(request);
            requestFutures.forEach(requestFuture -> {
                ListenableFuture<Void> future = requestFuture.setCallback(new RetainedMessageAddReplicateRequestCallback(this));
                futures.add(future);
            });
            return ClusterFutures.merge(futures);
        } catch (Throwable e) {
            return Futures.immediateFailedFuture(e);
        }
    }

    public ImmutableSet<ListenableFuture<Set<String>>> getLocalWithWildcards(@NotNull String topic) {
        try {
            Preconditions.checkNotNull(topic, "Topic must not be null");
            ImmutableSet.Builder<ListenableFuture<Set<String>>> builder = ImmutableSet.builder();
            for (int bucket = 0; bucket < this.bucketCount; bucket++) {
                int bucketIndex = bucket;
                builder.add(this.persistenceExecutors.get(bucket).add(() ->
                        retainedMessagesLocalPersistence.getTopics(
                                new LocalMatchTopicFilter(topic, topicMatcher, primaryRing, clusterConnection.getClusterId()), bucketIndex)
                ));
            }
            return builder.build();
        } catch (Throwable e) {
            return ImmutableSet.of(Futures.immediateFailedFuture(e));
        }
    }

    public ListenableFuture<ImmutableSet<ClusterReplicateRequest>> getDataForReplica(@NotNull Filter filter) {
        try {
            Preconditions.checkNotNull(filter, "Filter must not be null");
            Set<ListenableFuture<ImmutableSet<ClusterReplicateRequest>>> futures = new ConcurrentHashSet<>();
            for (int bucket = 0; bucket < this.bucketCount; bucket++) {
                int bucketIndex = bucket;
                futures.add(this.persistenceExecutors.get(bucket).add(() -> {
                    Map<String, TimestampObject<RetainedMessage>> retainedMessages = retainedMessagesLocalPersistence.getEntries(filter, bucketIndex);
                    ImmutableSet.Builder<ClusterReplicateRequest> builder = ImmutableSet.builder();
                    retainedMessages.forEach((topic, timestampObject) -> {
                        long timestamp = timestampObject.getTimestamp();
                        VectorClock localVectorClock = vectorClocks.get(topic);
                        ClusterReplicateRequest request;
                        if (timestampObject.getObject() == null) {
                            request = new RetainedMessageRemoveReplicateRequest(timestamp, localVectorClock, topic);
                        } else {
                            request = new RetainedMessageAddReplicateRequest(timestamp, localVectorClock, topic, timestampObject.getObject());
                        }
                        builder.add(request);
                    });
                    return builder.build();
                }));
            }
            SettableFuture<ImmutableSet<ClusterReplicateRequest>> settableFuture = SettableFuture.create();
            ClusterFutures.addCallback(Futures.allAsList(futures), new FutureCallback<List<ImmutableSet<ClusterReplicateRequest>>>() {

                @Override
                public void onSuccess(@javax.annotation.Nullable List<ImmutableSet<ClusterReplicateRequest>> result) {
                    if (result == null) {
                        settableFuture.set(ImmutableSet.of());
                        return;
                    }
                    ImmutableSet.Builder<ClusterReplicateRequest> builder = ImmutableSet.builder();
                    result.forEach(builder::addAll);
                    settableFuture.set(builder.build());
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

    @Override
    public ListenableFuture<Void> removeLocally(@NotNull Filter filter) {
        try {
            Preconditions.checkNotNull(filter, "Filter must not be null");
            ConcurrentHashSet<ListenableFuture<Void>> futures = new ConcurrentHashSet<>();
            for (int bucket = 0; bucket < this.bucketCount; bucket++) {
                int bucketIndex = bucket;
                futures.add(this.persistenceExecutors.get(bucket).add(() -> {
                    Set<String> topics = retainedMessagesLocalPersistence.removeAll(filter, bucketIndex);
                    topics.forEach(vectorClocks::remove);
                    return null;
                }));
            }
            return ClusterFutures.merge(futures);
        } catch (Throwable e) {
            return Futures.immediateFailedFuture(e);
        }
    }

    public ListenableFuture<Void> remove(@NotNull String topic, @NotNull VectorClock requestVectorClock, long requestTimestamp) {
        try {
            Preconditions.checkNotNull(topic, "Topic must not be null");
            Preconditions.checkNotNull(requestVectorClock, "Vector clock must not be null");
            return getExecutor(topic).add(() -> {
                replicateUpdate(topic, null, requestVectorClock, requestTimestamp);
                return null;
            });
        } catch (Throwable e) {
            return Futures.immediateFailedFuture(e);
        }
    }

    public ListenableFuture<Void> persist(@NotNull String topic, @Nullable RetainedMessage retainedMessage, @NotNull VectorClock requestVectorClock, long requestTimestamp) {
        try {
            Preconditions.checkNotNull(topic, "Topic must not be null");
            Preconditions.checkNotNull(requestVectorClock, "Vector clock must not be null");
            return getExecutor(topic).add(() -> {
                replicateUpdate(topic, retainedMessage, requestVectorClock, requestTimestamp);
                return null;
            });
        } catch (Throwable e) {
            return Futures.immediateFailedFuture(e);
        }
    }

    public ListenableFuture<Void> cleanUp(long tombstoneMaxAge) {
        List<ListenableFuture<Void>> futures = new ArrayList<>();
        for (int bucket = 0; bucket < this.bucketCount; bucket++) {
            int bucketIndex = bucket;
            futures.add(this.persistenceExecutors.get(bucket).add(() -> {
                Set<String> retainedMessages = retainedMessagesLocalPersistence.cleanUp(tombstoneMaxAge, bucketIndex);
                retainedMessages.forEach(vectorClocks::remove);
                return null;
            }));
        }
        return ClusterFutures.merge(futures);
    }

    private void replicateUpdate(@NotNull String topic, @Nullable RetainedMessage requestRetainedMessage, @NotNull VectorClock requestVectorClock, long requestTimestamp) {
        VectorClock localVectorClock = this.vectorClocks.get(topic);
        if (requestVectorClock.equals(localVectorClock) ||
                requestVectorClock.before(localVectorClock)) {
            return;
        }
        if (localVectorClock.before(requestVectorClock)) {
            this.vectorClocks.put(topic, requestVectorClock);
            if (requestRetainedMessage == null) {
                this.retainedMessagesLocalPersistence.remove(topic, requestTimestamp);
            } else {
                this.retainedMessagesLocalPersistence.addOrReplace(topic, requestRetainedMessage, requestTimestamp);
            }
            return;
        }
        LOGGER.info("Conflict for topic: {}. Local vector clock={}, request vector clock={}.",
                topic, localVectorClock, requestVectorClock);
        localVectorClock.merge(requestVectorClock);
        localVectorClock.increment(this.clusterConnection.getClusterId());
        this.vectorClocks.put(topic, localVectorClock);
        RetainedMessage localRetainedMessage = this.retainedMessagesLocalPersistence.get(topic);
        Long localTimestamp = this.retainedMessagesLocalPersistence.getTimestamp(topic);
        if (localTimestamp == null) {
            if (requestRetainedMessage == null) {
                this.retainedMessagesLocalPersistence.remove(topic, requestTimestamp);
            } else {
                this.retainedMessagesLocalPersistence.addOrReplace(topic, requestRetainedMessage, requestTimestamp);
            }
        } else {
            TimestampObject timestampObject = merge(localRetainedMessage, requestRetainedMessage, localTimestamp, requestTimestamp);
            if (timestampObject.getObject() == null) {
                this.retainedMessagesLocalPersistence.remove(topic, timestampObject.getTimestamp());
            } else {
                this.retainedMessagesLocalPersistence.addOrReplace(topic, (RetainedMessage) timestampObject.getObject(), timestampObject.getTimestamp());
            }
        }
    }


    private TimestampObject<RetainedMessage> merge(RetainedMessage localRetainedMessage,
                                                   RetainedMessage requestRetainedMessage,
                                                   long localTimestamp,
                                                   long requestTimestamp) {
        long timestamp;
        RetainedMessage retainedMessage;
        if (localTimestamp > requestTimestamp) {
            timestamp = localTimestamp;
            retainedMessage = localRetainedMessage;
        } else if (requestTimestamp > localTimestamp) {
            timestamp = requestTimestamp;
            retainedMessage = requestRetainedMessage;
        } else if (localRetainedMessage.hashCode() > requestRetainedMessage.hashCode()) {
            timestamp = localTimestamp;
            retainedMessage = localRetainedMessage;
        } else {
            timestamp = requestTimestamp;
            retainedMessage = requestRetainedMessage;
        }
        return new TimestampObject(retainedMessage, timestamp);
    }


    private PersistenceExecutor getExecutor(String topic) {
        return this.persistenceExecutors.get(BucketUtils.bucket(topic, this.bucketCount));
    }

    protected RetainedMessage get(String key) {
        return this.retainedMessagesLocalPersistence.get(key);
    }

    public ListenableFuture<Void> clear() {
        clearLocally();
        RetainedMessageClearRequest request = new RetainedMessageClearRequest();
        return this.clusterConnection.send(request, Void.class, RetainedMessageClearRequestCallback::new, true);
    }

    public ListenableFuture<Void> clearLocally() {
        List<ListenableFuture<Void>> futures = new ArrayList<>();
        for (int bucket = 0; bucket < this.bucketCount; bucket++) {
            int bucketIndex = bucket;
            futures.add(this.persistenceExecutors.get(bucket).add(() -> {
                retainedMessagesLocalPersistence.clear(bucketIndex);
                Set<String> clients = vectorClocks.getVectorClocks();
                clients.stream()
                        .filter(clientId -> bucketIndex == BucketUtils.bucket(clientId, bucketCount))
                        .forEach(clientId -> vectorClocks.remove(clientId));
                return null;
            }));
        }
        return ClusterFutures.merge(futures);
    }

    public String toString() {
        return this.vectorClocks.toString() + "\n" + this.retainedMessagesLocalPersistence.toString();
    }

    protected String name() {
        return "retained message";
    }

    protected int getReplicateCount() {
        return this.replicateCount;
    }
}
