package bb1;

import av.PersistenceConfigurationService;
import bc1.ClientSessionLocalPersistence;
import be1.ClientSessionLocalMemoryPersistence;
import bi1.ClientSessionLocalXodusPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Provider;

public class ClientSessionLocalPersistenceProvider implements Provider<ClientSessionLocalPersistence> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientSessionLocalPersistenceProvider.class);
    private final PersistenceConfigurationService persistenceConfigurationService;
    private final Provider<ClientSessionLocalXodusPersistence> exodusPersistenceProvider;
    private final Provider<ClientSessionLocalMemoryPersistence> memoryPersistenceProvider;

    @Inject
    ClientSessionLocalPersistenceProvider(
            PersistenceConfigurationService persistenceConfigurationService,
            Provider<ClientSessionLocalXodusPersistence> exodusPersistenceProvider,
            Provider<ClientSessionLocalMemoryPersistence> memoryPersistenceProvider) {
        this.persistenceConfigurationService = persistenceConfigurationService;
        this.exodusPersistenceProvider = exodusPersistenceProvider;
        this.memoryPersistenceProvider = memoryPersistenceProvider;
    }

    @Override
    public ClientSessionLocalPersistence get() {
        if (this.persistenceConfigurationService.getClientSessionGeneralMode() == PersistenceConfigurationService.PersistenceMode.IN_MEMORY) {
            LOGGER.trace("Using in-memory Client Session Persistence store");
            return this.memoryPersistenceProvider.get();
        }
        LOGGER.trace("Using file based Client Session Persistence store");
        return this.exodusPersistenceProvider.get();
    }
}
