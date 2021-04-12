package m;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HeartbeatConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(HeartbeatConfig.class);
    private boolean enabled;
    private int interval;
    private int timeout;

    public boolean isEnabled() {
        return this.enabled;
    }

    public void setEnabled(boolean enabled) {
        LOGGER.debug("Setting cluster heartbeat {}", enabled ? "enabled" : "disabled");
        this.enabled = enabled;
    }

    public int getInterval() {
        return this.interval;
    }

    public void setInterval(int interval) {
        LOGGER.debug("Setting cluster heartbeat interval to {} ms", interval);
        this.interval = interval;
    }

    public int getTimeout() {
        return this.timeout;
    }

    public void setTimeout(int timeout) {
        LOGGER.debug("Setting the cluster heartbeat timeout to {} ms", timeout);
        this.timeout = timeout;
    }
}
