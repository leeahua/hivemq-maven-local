package u;

import ak.VectorClock;
import com.google.common.base.Preconditions;
import com.hivemq.spi.annotations.NotNull;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class VectorClocks {
    private final Map<String, VectorClock> vectorClocks = new ConcurrentHashMap<>();

    @NotNull
    public VectorClock getAndIncrement(@NotNull String key, @NotNull String nodeId) {
        Preconditions.checkNotNull(key, "Key must not be null");
        Preconditions.checkNotNull(nodeId, "Node ID must not be null");
        if (this.vectorClocks.get(key) == null) {
            this.vectorClocks.put(key, new VectorClock());
        }
        this.vectorClocks.get(key).increment(nodeId);
        return this.vectorClocks.get(key);
    }

    @NotNull
    public VectorClock get(@NotNull String key) {
        Preconditions.checkNotNull(key, "Key must not be null");
        VectorClock vectorClock = this.vectorClocks.get(key);
        if (vectorClock == null) {
            return new VectorClock();
        }
        return vectorClock;
    }

    public void put(@NotNull String key, @NotNull VectorClock vectorClock) {
        Preconditions.checkNotNull(key, "Key must not be null");
        Preconditions.checkNotNull(vectorClock, "Vector clock must not be null");
        this.vectorClocks.put(key, vectorClock);
    }

    public void remove(@NotNull String key) {
        Preconditions.checkNotNull(key, "Key must not be null");
        this.vectorClocks.remove(key);
    }

    public Set<String> getVectorClocks() {
        return this.vectorClocks.keySet();
    }

    public String toString() {
        return "Vector clock: " + this.vectorClocks.toString();
    }
}
