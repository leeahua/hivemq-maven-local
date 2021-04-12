package x;

import av.InternalConfigurationService;
import av.Internals;
import bh1.BucketUtils;
import bz.RetainedMessage;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.hivemq.spi.annotations.NotNull;
import u.Filter;
import u.TimestampObject;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class RetainedMessagesLocalMemoryPersistence implements RetainedMessagesLocalPersistence {
    private final InternalConfigurationService internalConfigurationService;
    private final List<ConcurrentHashMap<String, TimestampObject<RetainedMessage>>> buckets;
    private int bucketCount;

    @Inject
    public RetainedMessagesLocalMemoryPersistence(InternalConfigurationService internalConfigurationService) {
        this.internalConfigurationService = internalConfigurationService;
        this.buckets = new ArrayList<>();
    }

    @PostConstruct
    protected void init() {
        this.bucketCount = this.internalConfigurationService.getInt(Internals.PERSISTENCE_RETAINED_MESSAGES_BUCKET_COUNT);
        for (int bucket = 0; bucket < this.bucketCount; bucket++) {
            this.buckets.add(new ConcurrentHashMap<>());
        }
    }

    public long size() {
        return this.buckets.stream()
                .mapToInt(Map::size)
                .sum();
    }

    public void clear(int bucketIndex) {
        this.buckets.get(bucketIndex).clear();
    }

    public void remove(@NotNull String topic, long timestamp) {
        Preconditions.checkNotNull(topic, "Topic must not be null");
        getBucket(topic).put(topic, new TimestampObject(null, timestamp));
    }

    public RetainedMessage get(@NotNull String topic) {
        Preconditions.checkNotNull(topic, "Topic must not be null");
        TimestampObject<RetainedMessage> timestampObject = getBucket(topic).get(topic);
        if (timestampObject != null) {
            return timestampObject.getObject();
        }
        return null;
    }

    public Long getTimestamp(@NotNull String topic) {
        Preconditions.checkNotNull(topic, "Topic must not be null");
        TimestampObject timestampObject = getBucket(topic).get(topic);
        if (timestampObject != null) {
            return timestampObject.getTimestamp();
        }
        return null;
    }

    public void addOrReplace(@NotNull String topic, @NotNull RetainedMessage retainedMessage, long timestamp) {
        Preconditions.checkNotNull(topic, "Topic must not be null");
        Preconditions.checkNotNull(retainedMessage, "Retained message must not be null");
        getBucket(topic).put(topic, new TimestampObject(retainedMessage, timestamp));
    }

    public Map<String, TimestampObject<RetainedMessage>> getEntries(@NotNull Filter filter, int bucketIndex) {
        Preconditions.checkNotNull(filter, "Filter must not be null");
        Preconditions.checkArgument(bucketIndex >= 0 && bucketIndex < this.bucketCount, "Bucket index out of range");
        ImmutableMap.Builder<String, TimestampObject<RetainedMessage>> builder = ImmutableMap.builder();
        this.buckets.get(bucketIndex).entrySet().stream()
                .filter(entry -> filter.test(entry.getKey()))
                .forEach(builder::put);
        return builder.build();
    }

    public Set<String> getTopics(@NotNull Filter filter, int bucketIndex) {
        Preconditions.checkNotNull(filter, "Filter must not be null");
        Preconditions.checkArgument(bucketIndex >= 0 && bucketIndex < this.bucketCount, "Bucket index out of range");
        ImmutableSet.Builder<String> builder = ImmutableSet.builder();
        this.buckets.get(bucketIndex).entrySet().stream()
                .filter(entry -> filter.test(entry.getKey()) &&
                        entry.getValue().getObject() != null)
                .forEach(entry -> builder.add(entry.getKey()));
        return builder.build();
    }

    public Set<String> removeAll(@NotNull Filter filter, int bucketIndex) {
        Preconditions.checkNotNull(filter, "Filter must not be null");
        Preconditions.checkArgument(bucketIndex >= 0 && bucketIndex < this.bucketCount, "Bucket index out of range");
        Set<String> topics = new HashSet<>();
        this.buckets.get(bucketIndex).entrySet().stream()
                .filter(entry -> filter.test(entry.getKey()))
                .forEach(entry -> {
                    this.buckets.remove(entry.getKey());
                    topics.add(entry.getKey());
                });
        return topics;
    }

    public Set<String> cleanUp(long tombstoneMaxAge, int bucketIndex) {
        ImmutableSet.Builder<String> builder = ImmutableSet.builder();
        this.buckets.get(bucketIndex).entrySet()
                .forEach(entry -> {
                    String topic = entry.getKey();
                    TimestampObject<RetainedMessage> timestampObject = entry.getValue();
                    if (timestampObject == null) {
                        builder.add(topic);
                    } else if (timestampObject.getObject() == null &&
                            System.currentTimeMillis() - timestampObject.getTimestamp() > tombstoneMaxAge) {
                        this.buckets.get(bucketIndex).remove(topic);
                        builder.add(topic);
                    }
                });
        return builder.build();
    }

    public String toString() {
        return "RetainedMessageLocalMemoryPersistenceImpl{buckets=" + this.buckets + '}';
    }

    private ConcurrentHashMap<String, TimestampObject<RetainedMessage>> getBucket(String topic) {
        return this.buckets.get(BucketUtils.bucket(topic, this.bucketCount));
    }

    public void close() {
    }
}
