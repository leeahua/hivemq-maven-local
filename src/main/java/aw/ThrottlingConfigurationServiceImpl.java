package aw;

import com.hivemq.spi.services.configuration.ThrottlingConfigurationService;
import com.hivemq.spi.services.configuration.ValueChangedCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Singleton
public class ThrottlingConfigurationServiceImpl implements ThrottlingConfigurationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ThrottlingConfigurationServiceImpl.class);
    private long maxConnections;
    private int maxMessageSize;
    private long outgoingLimit;
    private long incomingLimit;
    private ReadWriteLock maxConnectionsLock = new ReentrantReadWriteLock(true);
    private ReadWriteLock maxMessageSizeLock = new ReentrantReadWriteLock(true);
    private ReadWriteLock outgoingLimitLock = new ReentrantReadWriteLock(true);
    private ReadWriteLock incomingLimitLock = new ReentrantReadWriteLock(true);
    private final List<ValueChangedCallback<Long>> maxConnectionsChangedCallbacks = new CopyOnWriteArrayList<>();
    private final List<ValueChangedCallback<Integer>> maxMessageSizeChangedCallbacks = new CopyOnWriteArrayList<>();
    private final List<ValueChangedCallback<Long>> outgoingLimitChangedCallbacks = new CopyOnWriteArrayList<>();
    private final List<ValueChangedCallback<Long>> incomingLimitChangedCallbacks = new CopyOnWriteArrayList<>();

    public long maxConnections() {
        Lock lock = this.maxConnectionsLock.readLock();
        lock.lock();
        try {
            return this.maxConnections;
        } finally {
            lock.unlock();
        }
    }

    public int maxMessageSize() {
        Lock lock = this.maxMessageSizeLock.readLock();
        lock.lock();
        try {
            return this.maxMessageSize;
        } finally {
            lock.unlock();
        }
    }

    public long outgoingLimit() {
        Lock lock = this.outgoingLimitLock.readLock();
        lock.lock();
        try {
            return this.outgoingLimit;
        } finally {
            lock.unlock();
        }
    }

    public long incomingLimit() {
        Lock lock = this.incomingLimitLock.readLock();
        lock.lock();
        try {
            return this.incomingLimit;
        } finally {
            lock.unlock();
        }
    }

    public synchronized void setMaxConnections(long maxConnections) {
        Lock lock = this.maxConnectionsLock.writeLock();
        lock.lock();
        try {
            LOGGER.debug("Setting global maximum allowed connections to {}", maxConnections);
            this.maxConnections = maxConnections;
            this.maxConnectionsChangedCallbacks.forEach(callback -> callback.valueChanged(maxConnections));
        } finally {
            lock.unlock();
        }
    }

    public synchronized void setMaxMessageSize(int maxMessageSize) {
        Lock lock = this.maxMessageSizeLock.writeLock();
        lock.lock();
        try {
            LOGGER.debug("Setting global maximum allowed message size to {} bytes", maxMessageSize);
            this.maxMessageSize = maxMessageSize;
            this.maxMessageSizeChangedCallbacks.forEach(callback -> callback.valueChanged(maxMessageSize));
        } finally {
            lock.unlock();
        }
    }

    public synchronized void setOutgoingLimit(long outgoingLimit) {
        Lock lock = this.outgoingLimitLock.writeLock();
        lock.lock();
        try {
            LOGGER.debug("Throttling the global outgoing traffic limit {} bytes/second", outgoingLimit);
            this.outgoingLimit = outgoingLimit;
            this.outgoingLimitChangedCallbacks.forEach(callback -> callback.valueChanged(outgoingLimit));
        } finally {
            lock.unlock();
        }
    }

    public synchronized void setIncomingLimitLock(long incomingLimit) {
        Lock lock = this.incomingLimitLock.writeLock();
        lock.lock();
        try {
            LOGGER.debug("Throttling the global incoming traffic limit {} bytes/second", Long.valueOf(incomingLimit));
            this.incomingLimit = incomingLimit;
            this.incomingLimitChangedCallbacks.forEach(callback -> callback.valueChanged(incomingLimit));
        } finally {
            lock.unlock();
        }
    }

    public void maxConnectionsChanged(ValueChangedCallback<Long> callback) {
        this.maxConnectionsChangedCallbacks.add(callback);
    }

    public void maxMessageSizeChanged(ValueChangedCallback<Integer> callback) {
        this.maxMessageSizeChangedCallbacks.add(callback);
    }

    public void outgoingLimitChanged(ValueChangedCallback<Long> callback) {
        this.outgoingLimitChangedCallbacks.add(callback);
    }

    public void incomingLimitChanged(ValueChangedCallback<Long> callback) {
        this.incomingLimitChangedCallbacks.add(callback);
    }
}
