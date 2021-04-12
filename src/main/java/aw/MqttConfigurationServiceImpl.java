package aw;

import com.hivemq.spi.services.configuration.MqttConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Singleton
public class MqttConfigurationServiceImpl implements MqttConfigurationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(MqttConfigurationServiceImpl.class);
    private AtomicInteger maxClientIdLength = new AtomicInteger();
    private AtomicInteger retryInterval = new AtomicInteger();
    private AtomicLong maxQueuedMessages = new AtomicLong();
    private AtomicLong noConnectIdleTimeoutMillis = new AtomicLong();

    public int maxClientIdLength() {
        return this.maxClientIdLength.get();
    }

    public int retryInterval() {
        return this.retryInterval.get();
    }

    public long maxQueuedMessages() {
        return this.maxQueuedMessages.get();
    }

    public long noConnectIdleTimeoutMillis() {
        return this.noConnectIdleTimeoutMillis.get();
    }

    public void setMaxClientIdLength(int maxClientIdLength) {
        LOGGER.debug("Setting the maximum client id length to {} bytes",
                maxClientIdLength);
        this.maxClientIdLength.set(maxClientIdLength);
    }

    public void setRetryInterval(int retryInterval) {
        LOGGER.debug("Setting the retry interval for MQTT QoS message resending to {} seconds",
                retryInterval);
        this.retryInterval.set(retryInterval);
    }

    public void setMaxQueuedMessages(long maxQueuedMessages) {
        LOGGER.debug("Setting the number of max queued messages to {} entries",
                maxQueuedMessages);
        this.maxQueuedMessages.set(maxQueuedMessages);
    }

    public void setNoConnectIdleTimeoutMillis(long noConnectIdleTimeoutMillis) {
        LOGGER.debug("Setting the timeout for disconnecting idle tcp connections before a connect message was received to {} milliseconds",
                noConnectIdleTimeoutMillis);
        this.noConnectIdleTimeoutMillis.set(noConnectIdleTimeoutMillis);
    }
}
