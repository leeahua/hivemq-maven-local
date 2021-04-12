package ah;

import aj.ClusterFutures;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import d.CacheScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import s.Cluster;

import javax.inject.Inject;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

@CacheScoped
public class ClusterStateService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterStateService.class);
    private final Map<ClusterState, Set<ClusterStateChangedListener>> stateListeners;
    private final Map<String, ClusterState> clusterNodeStates = new ConcurrentHashMap<>();
    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock(true);
    private final ListeningExecutorService clusterExecutor;

    @Inject
    ClusterStateService(@Cluster ListeningExecutorService clusterExecutor) {
        this.clusterExecutor = clusterExecutor;
        this.stateListeners = new ConcurrentHashMap<>();
        for (ClusterState state : ClusterState.values()) {
            this.stateListeners.put(state, new HashSet<>());
        }
    }

    public ClusterState getState(String node) {
        Lock lock = this.readWriteLock.readLock();
        lock.lock();
        try {
            return this.clusterNodeStates.get(node);
        } finally {
            lock.unlock();
        }
    }

    public void change(String node, ClusterState newState) {
        Lock lock = this.readWriteLock.writeLock();
        lock.lock();
        try {
            LOGGER.debug("State of node {} changed to {} was {}.",
                    node, newState, this.clusterNodeStates.get(node));
            this.clusterNodeStates.put(node, newState);
            notifyStateChanged(newState, node);
        } finally {
            lock.unlock();
        }
    }

    public boolean change(String node, ClusterState state, Condition condition) {
        Lock lock = this.readWriteLock.writeLock();
        lock.lock();
        try {
            boolean match = condition.test(this.clusterNodeStates);
            if (match) {
                change(node, state);
            }
            return match;
        } finally {
            lock.unlock();
        }
    }

    public void setState(String node, ClusterState state) {
        Lock lock = this.readWriteLock.writeLock();
        lock.lock();
        try {
            LOGGER.debug("State of node {} set to {}", node, state);
            this.clusterNodeStates.put(node, state);
        } finally {
            lock.unlock();
        }
    }

    public void remove(String node) {
        Lock lock = this.readWriteLock.writeLock();
        lock.lock();
        try {
            this.clusterNodeStates.remove(node);
            notifyStateChanged(ClusterState.DEAD, node);
            LOGGER.debug("Removed state of node {}.", node);
        } finally {
            lock.unlock();
        }
    }

    public boolean containsNode(String node) {
        return this.clusterNodeStates.containsKey(node);
    }

    public Map<String, ClusterState> getClusterNodeStates() {
        return this.clusterNodeStates;
    }


    public void register(ClusterState state, ClusterStateChangedListener listener) {
        synchronized (this.stateListeners) {
            this.stateListeners.get(state).add(listener);
        }
    }

    public void register(ClusterStateChangedListener listener) {
        synchronized (this.stateListeners) {
            this.stateListeners.values().forEach(listeners -> listeners.add(listener));
        }
    }

    private void notifyStateChanged(ClusterState state, String node) {
        synchronized (this.stateListeners) {
            this.stateListeners.get(state).forEach(listener -> {
                ListenableFuture future = this.clusterExecutor.submit(
                        () -> listener.onChanged(state, node)
                );
                ClusterFutures.waitFuture(future);
            });
        }
    }

    public Set<String> getNodes(ClusterState... states) {
        Set<ClusterState> clusterStates = Sets.newHashSet(states);
        return this.clusterNodeStates.entrySet().stream()
                .filter(entry -> clusterStates.contains(entry.getValue()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    public String toString() {
        return "ClusterStateService{clusterNodeStates=" + this.clusterNodeStates + '}';
    }
}
