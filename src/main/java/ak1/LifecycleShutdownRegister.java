package ak1;

import ap.ShutdownRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

public class LifecycleShutdownRegister {
    private static final Logger LOGGER = LoggerFactory.getLogger(LifecycleShutdownRegister.class);
    private final ShutdownRegistry shutdownRegistry;
    private final LifecycleShutdown shutdown;
    
    @Inject
    LifecycleShutdownRegister(ShutdownRegistry shutdownRegistry,
                              LifecycleShutdown shutdown) {
        this.shutdownRegistry = shutdownRegistry;
        this.shutdown = shutdown;
    }

    @PostConstruct
    public void init() {
        this.shutdownRegistry.register(this.shutdown);
    }
}
