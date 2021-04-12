package l;

import k.ClusterDiscovery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PluginDiscovery extends ClusterDiscovery {
    private static final Logger LOGGER = LoggerFactory.getLogger(PluginDiscovery.class);
    private long interval;

    public long getInterval() {
        return this.interval;
    }

    public void setInterval(long interval) {
        LOGGER.debug("Set interval {} for plugin cluster discovery", interval);
        this.interval = interval;
    }

    public Type getType() {
        return Type.PLUGIN;
    }
}
