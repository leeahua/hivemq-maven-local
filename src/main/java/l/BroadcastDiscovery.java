package l;

import k.ClusterDiscovery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BroadcastDiscovery extends ClusterDiscovery {
    private static final Logger LOGGER = LoggerFactory.getLogger(BroadcastDiscovery.class);
    private int port;
    private String broadcastAddress;
    private int portRange;

    public int getPort() {
        return this.port;
    }

    public void setPort(int port) {
        LOGGER.debug("Set port {} for broadcast cluster discovery", port);
        this.port = port;
    }

    public String getBroadcastAddress() {
        return this.broadcastAddress;
    }

    public void setBroadcastAddress(String broadcastAddress) {
        LOGGER.debug("Set broadcast address {} for broadcast cluster discovery", broadcastAddress);
        this.broadcastAddress = broadcastAddress;
    }

    public int getPortRange() {
        return this.portRange;
    }

    public void setPortRange(int portRange) {
        LOGGER.debug("Set port range {} for broadcast cluster discovery", portRange);
        this.portRange = portRange;
    }

    public Type getType() {
        return Type.BROADCAST;
    }
}
