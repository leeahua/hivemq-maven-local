package ak;

import com.google.common.collect.Maps;
import com.hivemq.spi.annotations.ThreadSafe;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@ThreadSafe
public class VectorClock {
    private Map<String, Integer> vectors;

    public VectorClock() {
        this.vectors = new HashMap<>();
    }

    public VectorClock(HashMap<String, Integer> vectors) {
        this.vectors = vectors;
    }


    public synchronized void increment(String node) {
        if (this.vectors.containsKey(node)) {
            this.vectors.put(node, this.vectors.get(node) + 1);
        } else {
            this.vectors.put(node, 1);
        }
    }

    public synchronized Map<String, Integer> getVectors() {
        return Maps.newHashMap(this.vectors);
    }

    public synchronized void merge(VectorClock vectorClock) {
        vectorClock.getVectors().forEach((key, value) -> {
            if (!this.vectors.containsKey(key)) {
                this.vectors.put(key, value);
            } else {
                this.vectors.put(key, Math.max(value, this.vectors.get(key)));
            }
        });
    }

    public synchronized boolean before(VectorClock vectorClock) {
        boolean isBefore = false;
        Set<String> nodes = new HashSet();
        Map<String, Integer> thisVectors = getVectors();
        Map<String, Integer> otherVectors = vectorClock.getVectors();
        nodes.addAll(thisVectors.keySet());
        nodes.addAll(otherVectors.keySet());
        Iterator<String> iterator = nodes.iterator();
        while (iterator.hasNext()) {
            String node = iterator.next();
            int thisVector = Optional.ofNullable(thisVectors.get(node)).orElse(0);
            int otherVector = Optional.ofNullable(otherVectors.get(node)).orElse(0);
            if (thisVector > otherVector) {
                return false;
            }
            if (thisVector < otherVector) {
                isBefore = true;
            }
        }
        return isBefore;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        VectorClock vectorClock = (VectorClock) obj;
        return Objects.equals(this.vectors, vectorClock.vectors);
    }

    public int hashCode() {
        return Objects.hash(this.vectors);
    }

    public String toString() {
        return this.vectors.toString();
    }
}
