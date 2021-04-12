package q;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.hivemq.spi.annotations.NotNull;
import net.openhft.hashing.LongHashFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.util.HashSet;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Singleton
public class ConsistentHashingRing {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConsistentHashingRing.class);
    private final String name;
    public static final int NODE_BUCKET_COUNT = 500;
    private final LongHashFunction hashFunction;
    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock(true);
    @VisibleForTesting
    final NavigableMap<Long, String> buckets;
    @VisibleForTesting
    final ConcurrentHashMap<String, String> bucketNodes = new ConcurrentHashMap<>();
    final Set<String> nodes = Sets.newConcurrentHashSet();

    public ConsistentHashingRing(String name, LongHashFunction hashFunction) {
        this.name = name;
        this.buckets = new ConcurrentSkipListMap();
        this.hashFunction = hashFunction;
    }

    public void add(@NotNull String node) {
        Preconditions.checkNotNull(node, "Name must not be null");
        LOGGER.trace("Add node {} to the {}.", node, this.name);
        Lock lock = this.readWriteLock.writeLock();
        lock.lock();
        try {
            for (int bucketIndex = 0; bucketIndex < NODE_BUCKET_COUNT; bucketIndex++) {
                long bucketHash = this.hashFunction.hashChars(node + bucketIndex);
                if (this.buckets.containsKey(bucketHash)) {
                    if (this.buckets.get(bucketHash).compareTo(node + 1) > 0) {
                        this.buckets.put(bucketHash, node + bucketIndex);
                        this.nodes.add(node);
                        this.bucketNodes.put(node + bucketIndex, node);
                    }
                } else {
                    this.buckets.put(bucketHash, node + bucketIndex);
                    this.nodes.add(node);
                    this.bucketNodes.put(node + bucketIndex, node);
                }
            }
        } finally {
            lock.unlock();
        }
    }

    public void remove(@NotNull String node) {
        Preconditions.checkNotNull(node, "Name must not be null");
        LOGGER.trace("Remove node {} from the {}.", node, this.name);
        Lock lock = this.readWriteLock.writeLock();
        lock.lock();
        try {
            for (int bucketIndex = 0; bucketIndex < NODE_BUCKET_COUNT; bucketIndex++) {
                long bucketHash = this.hashFunction.hashChars(node + bucketIndex);
                this.buckets.remove(bucketHash);
                this.bucketNodes.remove(node + bucketIndex);
            }
            this.nodes.remove(node);
        } finally {
            lock.unlock();
        }
    }

    public Set<String> getReplicaNodes(@NotNull String key, int replicateCount) {
        Preconditions.checkNotNull(key, "key must not be null");
        int nodeCount = this.nodes.size();
        if (replicateCount > nodeCount - 1) {
            LOGGER.trace("There are not enough buckets in the consistent hash ring for {} replicas.", replicateCount);
            replicateCount = nodeCount - 1;
        }
        String bucket = getBucket(key);
        long bucketHash = this.hashFunction.hashChars(bucket);
        Lock lock = this.readWriteLock.readLock();
        lock.lock();
        Set<String> buckets = new HashSet<>();
        try {
            for (Map.Entry<Long, String> entry = this.buckets.higherEntry(bucketHash);
                 buckets.size() < replicateCount;
                 entry = this.buckets.higherEntry(entry.getKey())) {
                if (entry == null) {
                    entry = this.buckets.firstEntry();
                }
                if (!this.bucketNodes.get(entry.getValue()).equals(this.bucketNodes.get(bucket))) {
                    buckets.add(this.bucketNodes.get(entry.getValue()));
                }
            }
            return buckets;
        } finally {
            lock.unlock();
        }
    }

    public Set<String> getNodes() {
        ImmutableSet.Builder<String> builder = ImmutableSet.builder();
        Lock lock = this.readWriteLock.readLock();
        lock.lock();
        try {
            return builder.addAll(this.nodes).build();
        } finally {
            lock.unlock();
        }
    }

    public String getBucket(@NotNull String key) {
        Preconditions.checkNotNull(key, "key must not be null");
        if (this.buckets.isEmpty()) {
            throw new IllegalStateException("Consistent hash ring is empty.");
        }
        long keyHash = this.hashFunction.hashChars(key);
        Lock lock = this.readWriteLock.readLock();
        lock.lock();
        try {
            Map.Entry<Long, String> entry = this.buckets.ceilingEntry(keyHash);
            if (entry != null) {
                return entry.getValue();
            }
            return this.buckets.ceilingEntry(Long.MIN_VALUE).getValue();
        } finally {
            lock.unlock();
        }
    }

    public String getNode(@NotNull String key) {
        Preconditions.checkNotNull(key, "key must not be null");
        if (this.buckets.isEmpty()) {
            throw new IllegalStateException("Consistent hash ring is empty.");
        }
        long keyHash = this.hashFunction.hashChars(key);
        Lock lock = this.readWriteLock.readLock();
        lock.lock();
        try {
            Map.Entry<Long, String> entry = this.buckets.ceilingEntry(keyHash);
            if (entry != null) {
                return this.bucketNodes.get(entry.getValue());
            }
            return this.bucketNodes.get(this.buckets.ceilingEntry(Long.MIN_VALUE).getValue());
        } finally {
            lock.unlock();
        }
    }

    public static void main(String[] args) {
        System.out.printf("ddd");
    }
}
