package m;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TcpHealthCheckConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(TcpHealthCheckConfig.class);
    private boolean enabled;
    private String bindAddress;
    private int bindPort;
    private int portRange;

    public boolean isEnabled() {
        return this.enabled;
    }

    public void setEnabled(boolean enabled) {
        LOGGER.debug("Setting cluster TCP health-check {}", enabled ? "enabled" : "disabled");
        this.enabled = enabled;
    }

    public String getBindAddress() {
        return this.bindAddress;
    }

    public void setBindAddress(String bindAddress) {
        LOGGER.debug("Setting cluster TCP health-check bind-adress to {}", bindAddress != null ? bindAddress : "default");
        this.bindAddress = bindAddress;
    }

    public int getBindPort() {
        return this.bindPort;
    }

    public void setBindPort(int bindPort) {
        LOGGER.debug("Setting cluster TCP health-check bind-port to {}", bindPort);
        this.bindPort = bindPort;
    }

    public int getPortRange() {
        return this.portRange;
    }

    public void setPortRange(int portRange) {
        LOGGER.debug("Setting cluster TCP health-check port-range to {}", portRange);
        this.portRange = portRange;
    }
}
