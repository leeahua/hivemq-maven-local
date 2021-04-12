package bb1;

import av.PersistenceConfigurationService;
import bg1.RetainedMessagesLocalXodusPersistence;
import d.CacheScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import x.RetainedMessagesLocalMemoryPersistence;
import x.RetainedMessagesLocalPersistence;

import javax.inject.Inject;
import javax.inject.Provider;

@CacheScoped
public class RetainedMessagesLocalPersistenceProvider
        implements Provider<RetainedMessagesLocalPersistence> {
    private static final Logger LOGGER = LoggerFactory.getLogger(RetainedMessagesLocalPersistenceProvider.class);
    private final PersistenceConfigurationService persistenceConfigurationService;
    private final Provider<RetainedMessagesLocalXodusPersistence> exodusPersistenceProvider;
    private final Provider<RetainedMessagesLocalMemoryPersistence> memoryPersistenceProvider;

    @Inject
    public RetainedMessagesLocalPersistenceProvider(
            PersistenceConfigurationService persistenceConfigurationService,
            Provider<RetainedMessagesLocalXodusPersistence> exodusPersistenceProvider,
            Provider<RetainedMessagesLocalMemoryPersistence> memoryPersistenceProvider) {
        this.persistenceConfigurationService = persistenceConfigurationService;
        this.exodusPersistenceProvider = exodusPersistenceProvider;
        this.memoryPersistenceProvider = memoryPersistenceProvider;
    }

    @Override
    public RetainedMessagesLocalPersistence get() {
        if (this.persistenceConfigurationService.getRetainedMessagesMode() == PersistenceConfigurationService.PersistenceMode.IN_MEMORY) {
            LOGGER.debug("Using in-memory Retained Message Persistence");
            return this.memoryPersistenceProvider.get();
        }
        LOGGER.debug("Using disk-based Retained Message Persistence");
        return this.exodusPersistenceProvider.get();
    }
}
