package o;

import k.ClusterTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClusterUdpTransport extends ClusterTransport {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterUdpTransport.class);
    private boolean multicastEnabled;
    private int multicastPort;
    private String multicastAddress;

    public ClusterUdpTransport(int bindPort, String bindAddress) {
        super(bindPort, bindAddress);
    }

    public Type getType() {
        return Type.UDP;
    }

    public boolean isMulticastEnabled() {
        return this.multicastEnabled;
    }

    public int getMulticastPort() {
        return this.multicastPort;
    }

    public String getMulticastAddress() {
        return this.multicastAddress;
    }

    public void setMulticastEnabled(boolean multicastEnabled) {
        LOGGER.debug("Set multicast {} for cluster UDP transport", multicastEnabled ? "enabled" : "disabled");
        this.multicastEnabled = multicastEnabled;
    }

    public void setMulticastPort(int multicastPort) {
        LOGGER.debug("Set multicast port {} for cluster UDP transport", multicastPort);
        this.multicastPort = multicastPort;
    }

    public void setMulticastAddress(String multicastAddress) {
        LOGGER.debug("Set multicast address {} for cluster UDP transport", multicastAddress);
        this.multicastAddress = multicastAddress;
    }
}
