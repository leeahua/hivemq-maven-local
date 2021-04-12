package bz;

import ap.ShutdownRegistry;
import com.google.inject.Inject;

import javax.annotation.PostConstruct;

public class PersistenceShutdownRegister {
    private final ShutdownRegistry shutdownRegistry;
    private final PersistenceShutdown shutdown;

    @Inject
    public PersistenceShutdownRegister(ShutdownRegistry shutdownRegistry, PersistenceShutdown shutdown) {
        this.shutdownRegistry = shutdownRegistry;
        this.shutdown = shutdown;
    }

    @PostConstruct
    public void init() {
        this.shutdownRegistry.register(this.shutdown);
    }
}
