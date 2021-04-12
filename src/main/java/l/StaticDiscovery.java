package l;

import k.ClusterDiscovery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class StaticDiscovery extends ClusterDiscovery {
    private static final Logger LOGGER = LoggerFactory.getLogger(StaticDiscovery.class);
    private List<DiscoveryNode> nodes = new ArrayList<>();

    public List<DiscoveryNode> getNodes() {
        return this.nodes;
    }

    public void setNodes(List<DiscoveryNode> nodes) {
        LOGGER.debug("Set nodes {} for static cluster discovery", nodes);
        this.nodes = nodes;
    }

    public Type getType() {
        return Type.STATIC;
    }
}
