package x;

import aj.ClusterFutures;
import ak.VectorClock;
import av.InternalConfigurationService;
import av.Internals;
import bh1.BucketUtils;
import bz.RetainedMessage;
import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.ListenableFuture;
import com.hivemq.spi.annotations.NotNull;
import com.hivemq.spi.annotations.Nullable;
import com.hivemq.spi.topic.TopicMatcher;
import d.CacheScoped;
import j1.ClusterReplicateRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import u.Filter;
import u.MatchTopicFilter;
import u.NotImplementedException;
import u.PersistenceExecutor;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@CacheScoped
public class RetainedMessagesSinglePersistenceImpl
        implements RetainedMessagesClusterPersistence, RetainedMessagesSinglePersistence {
    private static final Logger LOGGER = LoggerFactory.getLogger(RetainedMessagesSinglePersistenceImpl.class);
    private final RetainedMessagesLocalPersistence retainedMessagesLocalPersistence;
    private final TopicMatcher topicMatcher;
    private final InternalConfigurationService internalConfigurationService;
    private final MetricRegistry metricRegistry;
    private final List<PersistenceExecutor> persistenceExecutors;
    private int bucketCount;

    @Inject
    public RetainedMessagesSinglePersistenceImpl(
            RetainedMessagesLocalPersistence retainedMessagesLocalPersistence,
            TopicMatcher topicMatcher,
            InternalConfigurationService internalConfigurationService,
            MetricRegistry metricRegistry) {
        this.retainedMessagesLocalPersistence = retainedMessagesLocalPersistence;
        this.topicMatcher = topicMatcher;
        this.internalConfigurationService = internalConfigurationService;
        this.metricRegistry = metricRegistry;
        this.bucketCount = this.internalConfigurationService.getInt(Internals.PERSISTENCE_RETAINED_MESSAGES_BUCKET_COUNT);
        this.persistenceExecutors = new ArrayList<>(this.bucketCount);
        for (int bucket = 0; bucket < this.bucketCount; bucket++) {
            this.persistenceExecutors.add(new PersistenceExecutor("retained-message-writer-" + bucket, this.metricRegistry));
        }
    }

    public ListenableFuture<Void> clear() {
        List<ListenableFuture<Void>> futures = new ArrayList<>();
        for (int bucket = 0; bucket < this.bucketCount; bucket++) {
            int bucketIndex = bucket;
            this.persistenceExecutors.get(bucket).add(() -> {
                retainedMessagesLocalPersistence.clear(bucketIndex);
                return null;
            });
        }
        return ClusterFutures.merge(futures);
    }

    public long size() {
        return this.retainedMessagesLocalPersistence.size();
    }

    public ListenableFuture<Void> remove(@NotNull String topic) {
        return getExecutor(topic).add(() -> {
            retainedMessagesLocalPersistence.remove(topic, System.currentTimeMillis());
            return null;
        });
    }

    public ListenableFuture<RetainedMessage> getWithoutWildcards(@NotNull String topic) {
        return getExecutor(topic).add(() ->
                retainedMessagesLocalPersistence.get(topic));
    }

    public ListenableFuture<Void> addOrReplace(@NotNull String topic, @NotNull RetainedMessage retainedMessage) {
        return getExecutor(topic).add(() -> {
            retainedMessagesLocalPersistence.addOrReplace(topic, retainedMessage, System.currentTimeMillis());
            return null;
        });
    }

    @NotNull
    public ImmutableList<ListenableFuture<Set<String>>> getTopics(@NotNull String topic) {
        ImmutableList.Builder<ListenableFuture<Set<String>>> builder = ImmutableList.builder();
        for (int bucket = 0; bucket < this.bucketCount; bucket++) {
            int bucketIndex = bucket;
            builder.add(this.persistenceExecutors.get(bucket)
                    .add(() -> retainedMessagesLocalPersistence.getTopics(new MatchTopicFilter(topic, topicMatcher), bucketIndex)));
        }
        return builder.build();
    }

    public ListenableFuture<Void> cleanUp(long tombstoneMaxAge) {
        List<ListenableFuture<Void>> futures = new ArrayList<>();
        for (int bucket = 0; bucket < this.bucketCount; bucket++) {
            int bucketIndex = bucket;
            futures.add(this.persistenceExecutors.get(bucket).add(() -> {
                retainedMessagesLocalPersistence.cleanUp(tombstoneMaxAge, bucketIndex);
                return null;
            }));
        }
        return ClusterFutures.merge(futures);
    }

    public ListenableFuture<Void> clearLocally() {
        throw new NotImplementedException("Cluster persistence method 'clearLocally' not implemented in single instance persistence");
    }

    public ListenableFuture<Void> remove(@NotNull String topic, @NotNull VectorClock requestVectorClock, long requestTimestamp) {
        throw new NotImplementedException("Cluster persistence method 'remove' not implemented in single instance persistence");
    }

    public RetainedMessage getLocally(@NotNull String topic) {
        throw new NotImplementedException("Cluster persistence method 'getLocally' not implemented in single instance persistence");
    }

    public ImmutableSet<ListenableFuture<Set<String>>> getLocalWithWildcards(@NotNull String topic) {
        throw new NotImplementedException("Cluster persistence method 'getLocalWithWildcards' not implemented in single instance persistence");
    }

    public ListenableFuture<Void> persist(@NotNull String topic, @Nullable RetainedMessage retainedMessage, @NotNull VectorClock requestVectorClock, long requestTimestamp) {
        throw new NotImplementedException("Cluster persistence method 'persist' not implemented in single instance persistence");
    }

    public ListenableFuture<ImmutableSet<ClusterReplicateRequest>> getDataForReplica(@NotNull Filter filter) {
        throw new NotImplementedException("Cluster persistence method 'getDataForReplica' not implemented in single instance persistence");
    }

    public ListenableFuture<Void> removeLocally(@NotNull Filter filter) {
        throw new NotImplementedException("Cluster persistence method 'removeLocally' not implemented in single instance persistence");
    }

    public ListenableFuture<Void> replicateRemove(long requestTimestamp, @NotNull VectorClock requestVectorClock, @NotNull String topic) {
        throw new NotImplementedException("Cluster persistence method 'replicateRemove' not implemented in single instance persistence");
    }

    public ListenableFuture<Void> replicateAdd(long requestTimestamp, @NotNull VectorClock requestVectorClock, @NotNull String topic, @NotNull RetainedMessage retainedMessage) {
        throw new NotImplementedException("Cluster persistence method 'replicateAdd' not implemented in single instance persistence");
    }

    private PersistenceExecutor getExecutor(String topic) {
        return this.persistenceExecutors.get(BucketUtils.bucket(topic, this.bucketCount));
    }
}
