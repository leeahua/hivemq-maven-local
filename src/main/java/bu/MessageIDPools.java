package bu;

import av.InternalConfigurationService;
import av.Internals;
import bv.MessageIdProducer;
import bv.MessageIdProducerImpl;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.Striped;
import com.google.inject.Inject;
import com.hivemq.spi.annotations.NotNull;

import javax.inject.Singleton;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

@Singleton
public class MessageIDPools {
    private final Striped<ReadWriteLock> stripedLock;
    private final Map<String, MessageIdProducer> producers = new ConcurrentHashMap<>();

    @Inject
    MessageIDPools(InternalConfigurationService internalConfigurationService) {
        this.stripedLock = Striped.lazyWeakReadWriteLock(
                internalConfigurationService.getInt(Internals.MESSAGE_ID_PRODUCER_LOCK_SIZE));
    }

    @NotNull
    public MessageIdProducer forClient(String clientId) {
        Lock lock = this.stripedLock.get(clientId).readLock();
        lock.lock();
        try {
            MessageIdProducer producer = this.producers.get(clientId);
            if (producer == null) {
                this.producers.putIfAbsent(clientId, new MessageIdProducerImpl());
                producer = this.producers.get(clientId);
            }
            return producer;
        } finally {
            lock.unlock();
        }
    }

    public void remove(String clientId) {
        Lock lock = this.stripedLock.get(clientId).writeLock();
        lock.lock();
        try {
            this.producers.remove(clientId);
        } finally {
            lock.unlock();
        }
    }

    @VisibleForTesting
    protected int size() {
        return this.producers.size();
    }
}
