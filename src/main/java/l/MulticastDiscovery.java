package l;

import k.ClusterDiscovery;

public class MulticastDiscovery extends ClusterDiscovery {
    public Type getType() {
        return Type.MULTICAST;
    }
}
