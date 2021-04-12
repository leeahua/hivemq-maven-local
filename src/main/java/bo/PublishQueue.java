package bo;

import am1.Metrics;
import bu.InternalPublish;
import co.PublishServiceImpl;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.hivemq.spi.annotations.NotNull;
import d.CacheScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@CacheScoped
public class PublishQueue {
    private static final Logger LOGGER = LoggerFactory.getLogger(PublishQueue.class);
    public static final int BUCKET_COUNT = 64;
    private final List<Deque<InternalPublish>> publishQueueBuckets;
    private final List<Set<String>> publishIdsBuckets;
    private final List<ReadWriteLock> lockBuckets;
    private final Metrics metrics;
    private final PublishServiceImpl publishService;

    @Inject
    public PublishQueue(Metrics metrics, PublishServiceImpl publishService) {
        this.metrics = metrics;
        this.publishService = publishService;
        this.publishQueueBuckets = Lists.newArrayListWithCapacity(BUCKET_COUNT);
        this.lockBuckets = Lists.newArrayListWithCapacity(BUCKET_COUNT);
        this.publishIdsBuckets = Lists.newArrayListWithCapacity(BUCKET_COUNT);
        for (int bucket = 0; bucket < BUCKET_COUNT; bucket++) {
            this.publishQueueBuckets.add(new ArrayDeque<>());
            this.lockBuckets.add(new ReentrantReadWriteLock(true));
            this.publishIdsBuckets.add(new HashSet<>());
        }
    }

    public void enqueue(String clientId, InternalPublish publish) {
        Lock lock = getLock(clientId).writeLock();
        lock.lock();
        try {
            if (!getPublishQueue(clientId).offerLast(publish)) {
                LOGGER.debug("Not able to enqueue message for client {}", clientId);
            } else {
                this.metrics.publishQueueSize().inc();
                this.metrics.publishQueueRate().mark();
            }
        } finally {
            lock.unlock();
        }
    }

    public int size() {
        this.lockBuckets.forEach(lock -> lock.readLock().lock());
        try {
            return this.publishQueueBuckets.stream()
                    .mapToInt(publishQueue -> publishQueue.size())
                    .sum();
        } finally {
            this.lockBuckets.forEach(lock -> lock.readLock().unlock());
        }
    }

    public void dequeue(String clientId, InternalPublish publish,
                        ExecutorService executorService) {
        Lock lock = getLock(clientId).writeLock();
        lock.lock();
        try {
            Set<String> publishIds = getPublishIds(clientId);
            Deque<InternalPublish> publishQueue = getPublishQueue(clientId);
            InternalPublish first = publishQueue.peekFirst();
            if (first == null || !same(publish, first)) {
                publishIds.add(publish.getUniqueId());
                return;
            }
            this.metrics.publishQueueSize().dec();
            first = publishQueue.pop();
            this.publishService.publish(first, executorService);
            if (publishQueue.size() < 1) {
                return;
            }
            do {
                first = publishQueue.peekFirst();
                if (!publishIds.contains(first.getUniqueId())) {
                    break;
                }
                publishIds.remove(first.getUniqueId());
                first = publishQueue.pop();
                this.publishService.publish(first, executorService);
                this.metrics.publishQueueSize().dec();
            } while (publishQueue.size() > 0);
        } finally {
            lock.unlock();
        }
    }

    private boolean same(InternalPublish publish1, InternalPublish publish2) {
        return publish1.getClusterId().equals(publish2.getClusterId()) &&
                publish1.getSequence() == publish2.getSequence();
    }

    @NotNull
    private ReadWriteLock getLock(String clientId) {
        return this.lockBuckets.get(bucketIndex(clientId));
    }

    @NotNull
    private Deque<InternalPublish> getPublishQueue(String clientId) {
        return this.publishQueueBuckets.get(bucketIndex(clientId));
    }

    @NotNull
    private Set<String> getPublishIds(String clientId) {
        return this.publishIdsBuckets.get(bucketIndex(clientId));
    }

    private int bucketIndex(String clientId) {
        Preconditions.checkNotNull(clientId, "ClientID cannot be null");
        return Math.abs(clientId.hashCode() % BUCKET_COUNT);
    }
}
