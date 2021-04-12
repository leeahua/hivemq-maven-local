package bb1;

import av.PersistenceConfigurationService;
import bc1.IncomingMessageFlowLocalPersistence;
import be1.IncomingMessageFlowLocalMemoryPersistence;
import bg1.IncomingMessageFlowLocalXodusPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Provider;

public class IncomingMessageFlowLocalPersistenceProvider
        implements Provider<IncomingMessageFlowLocalPersistence> {
    private static final Logger LOGGER = LoggerFactory.getLogger(IncomingMessageFlowLocalPersistenceProvider.class);
    private final PersistenceConfigurationService persistenceConfigurationService;
    private final Provider<IncomingMessageFlowLocalMemoryPersistence> memoryPersistenceProvider;
    private final Provider<IncomingMessageFlowLocalXodusPersistence> exodusPersistenceProvider;

    @Inject
    IncomingMessageFlowLocalPersistenceProvider(PersistenceConfigurationService persistenceConfigurationService,
                                                Provider<IncomingMessageFlowLocalMemoryPersistence> paramProvider,
                                                Provider<IncomingMessageFlowLocalXodusPersistence> paramProvider1) {
        this.persistenceConfigurationService = persistenceConfigurationService;
        this.memoryPersistenceProvider = paramProvider;
        this.exodusPersistenceProvider = paramProvider1;
    }

    @Override
    public IncomingMessageFlowLocalPersistence get() {
        if (this.persistenceConfigurationService.getMessageFlowIncomingMode() == PersistenceConfigurationService.PersistenceMode.IN_MEMORY) {
            LOGGER.trace("Using in-memory Incoming Message Flow store");
            return this.memoryPersistenceProvider.get();
        }
        LOGGER.trace("Using file based Xodus Incoming Message Flow store");
        return this.exodusPersistenceProvider.get();
    }
}
