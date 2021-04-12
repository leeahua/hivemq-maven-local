package aq1;

import net.jcip.annotations.ThreadSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.io.Serializable;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@ThreadSafe
@Singleton
public class MqttConnectionCounter implements Serializable {
    private static final Logger LOGGER = LoggerFactory.getLogger(MqttConnectionCounter.class);
    private final Set<Callback> callbacks = new CopyOnWriteArraySet<>();
    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock(true);
    private long currentConnections = 0L;
    private long maxConcurrentConnections = 0L;

    public long currentConnections() {
        Lock lock = this.readWriteLock.readLock();
        lock.lock();
        try {
            return this.currentConnections;
        } finally {
            lock.unlock();
        }
    }

    public long maxConcurrentConnections() {
        Lock lock = this.readWriteLock.readLock();
        lock.lock();
        try {
            return this.maxConcurrentConnections;
        } finally {
            lock.unlock();
        }
    }

    public long increase() {
        Lock lock = this.readWriteLock.writeLock();
        lock.lock();
        try {
            long currentConnections = ++this.currentConnections;
            LOGGER.trace("Increasing connection count by one. New value: {}", currentConnections);
            if (currentConnections > this.maxConcurrentConnections) {
                LOGGER.trace("New maximum concurrent connection count: {}", currentConnections);
                this.maxConcurrentConnections = currentConnections;
            }
            fireConnectionsChanged(currentConnections);
            return currentConnections;
        } finally {
            lock.unlock();
        }
    }

    public long decrease() {
        Lock lock = this.readWriteLock.writeLock();
        lock.lock();
        try {
            this.currentConnections -= 1L;
            if (this.currentConnections < 0L) {
                LOGGER.warn("Tried to decrease the MQTT connection count although it's already zero.");
                this.currentConnections = 0L;
            }
            fireConnectionsChanged(this.currentConnections);
            return this.currentConnections;
        } finally {
            lock.unlock();
        }
    }

    public void register(Callback callback) {
        this.callbacks.add(callback);
    }

    private void fireConnectionsChanged(long newCount) {
        this.callbacks.forEach(callback -> callback.connectionsChanged(newCount));
    }

    public interface Callback {
        void connectionsChanged(long paramLong);
    }
}
