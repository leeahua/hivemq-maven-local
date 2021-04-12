package o;

import k.ClusterTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClusterTcpTransport extends ClusterTransport {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterTcpTransport.class);
    private String clientBindAddress;
    private int clientBindPort;

    public ClusterTcpTransport(int clientBindPort, String clientBindAddress) {
        super(clientBindPort, clientBindAddress);
    }

    @Override
    public Type getType() {
        return Type.TCP;
    }

    public String getClientBindAddress() {
        return clientBindAddress;
    }

    public int getClientBindPort() {
        return clientBindPort;
    }

    public void setClientBindAddress(String clientBindAddress) {
        LOGGER.debug("Set client bind address {} for cluster TCP transport", clientBindAddress);
        this.clientBindAddress = clientBindAddress;
    }

    public void setClientBindPort(int clientBindPort) {
        LOGGER.debug("Set client bind port {} for cluster TCP transport", clientBindPort);
        this.clientBindPort = clientBindPort;
    }
}
