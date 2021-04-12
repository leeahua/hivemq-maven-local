package ah;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.hivemq.spi.config.SystemInformation;
import d.CacheScoped;
import i.ClusterIdProducer;
import org.jgroups.Address;
import org.jgroups.util.UUID;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@CacheScoped
public class ClusterContext {
    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private final Map<String, NodeInformation> nodeInformations = new ConcurrentHashMap<>();

    @Inject
    public ClusterContext(SystemInformation systemInformation,
                          ClusterIdProducer clusterIdProducer) {
        this.nodeInformations.put(clusterIdProducer.get(),
                new NodeInformation(systemInformation.getHiveMQVersion(), null, System.currentTimeMillis()));
    }

    public Address getAddress(String node) {
        return UUID.getByName(node);
    }

    public void add(String node) {
        Lock lock = this.readWriteLock.writeLock();
        lock.lock();
        try {
            if (this.nodeInformations.get(node) == null) {
                this.nodeInformations.put(node, new NodeInformation());
                this.nodeInformations.get(node).setLastAddedMillis(System.currentTimeMillis());
            }
        } finally {
            lock.unlock();
        }
    }

    public void remove(String node) {
        Lock lock = this.readWriteLock.writeLock();
        lock.lock();
        try {
            putIfAbsent(node);
            this.nodeInformations.get(node).setLastRemovedMillis(System.currentTimeMillis());
        } finally {
            lock.unlock();
        }
    }

    public void setVersion(String node, String version) {
        Lock lock = this.readWriteLock.writeLock();
        lock.lock();
        try {
            putIfAbsent(node);
            this.nodeInformations.get(node).setVersion(version);
        } finally {
            lock.unlock();
        }
    }

    public String getVersion(String node) {
        Lock lock = this.readWriteLock.readLock();
        lock.lock();
        try {
            String str = this.nodeInformations.get(node).getVersion();
            return str;
        } finally {
            lock.unlock();
        }
    }

    public Long getLastAddedMillis(String node) {
        Lock lock = this.readWriteLock.readLock();
        lock.lock();
        try {
            return this.nodeInformations.get(node).getLastAddedMillis();
        } finally {
            lock.unlock();
        }
    }

    public Long getLastRemovedMillis(String node) {
        Lock lock = this.readWriteLock.readLock();
        lock.lock();
        try {
            return this.nodeInformations.get(node).getLastRemovedMillis();
        } finally {
            lock.unlock();
        }
    }

    public ImmutableMap<String, String> getNodeInformations() {
        Lock lock = this.readWriteLock.readLock();
        lock.lock();
        try {
            ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
            this.nodeInformations.entrySet().stream()
                    .filter(entry -> Objects.nonNull(entry.getValue().getVersion()))
                    .forEach(entry -> builder.put(entry.getKey(), entry.getValue().getVersion()));
            return builder.build();
        } finally {
            lock.unlock();
        }
    }

    private void putIfAbsent(String node) {
        this.nodeInformations.putIfAbsent(node, new NodeInformation());
    }
}
