package bb1;

import av.PersistenceConfigurationService;
import bc1.OutgoingMessageFlowLocalPersistence;
import bg1.OutgoingMessageFlowLocalXodusPersistence;
import bg1.OutgoingMessageFlowLocalMemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Provider;

public class OutgoingMessageFlowLocalPersistenceProvider
        implements Provider<OutgoingMessageFlowLocalPersistence> {
    private static final Logger LOGGER = LoggerFactory.getLogger(OutgoingMessageFlowLocalPersistenceProvider.class);
    private final PersistenceConfigurationService persistenceConfigurationService;
    private final Provider<OutgoingMessageFlowLocalXodusPersistence> exodusPersistenceProvider;
    private final Provider<OutgoingMessageFlowLocalMemoryPersistence> memoryPersistenceProvider;

    @Inject
    OutgoingMessageFlowLocalPersistenceProvider(
            PersistenceConfigurationService persistenceConfigurationService,
            Provider<OutgoingMessageFlowLocalXodusPersistence> exodusPersistenceProvider,
            Provider<OutgoingMessageFlowLocalMemoryPersistence> memoryPersistenceProvider) {
        this.persistenceConfigurationService = persistenceConfigurationService;
        this.exodusPersistenceProvider = exodusPersistenceProvider;
        this.memoryPersistenceProvider = memoryPersistenceProvider;
    }

    @Override
    public OutgoingMessageFlowLocalPersistence get() {
        if (this.persistenceConfigurationService.getMessageFlowOutgoingMode() == PersistenceConfigurationService.PersistenceMode.IN_MEMORY) {
            LOGGER.trace("Using in-memory Outgoing Messages store");
            return this.memoryPersistenceProvider.get();
        }
        LOGGER.trace("Using file based Outgoing Messages store");
        return this.exodusPersistenceProvider.get();
    }
}
