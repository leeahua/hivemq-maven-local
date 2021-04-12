package cc;

import ap.ShutdownRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

public class OnBrokerStopCallbacksShutdownRegister {
    private static final Logger LOGGER = LoggerFactory.getLogger(OnBrokerStopCallbacksShutdownRegister.class);
    private final ShutdownRegistry shutdownRegistry;
    private final OnBrokerStopCallbacksShutdown shutdown;

    @Inject
    OnBrokerStopCallbacksShutdownRegister(ShutdownRegistry shutdownRegistry,
                                          OnBrokerStopCallbacksShutdown shutdown) {
        this.shutdownRegistry = shutdownRegistry;
        this.shutdown = shutdown;
    }

    @PostConstruct
    public void init() {
        LOGGER.debug("Registering Shutdown hook for Broker Plugins");
        this.shutdownRegistry.register(this.shutdown);
    }
}
