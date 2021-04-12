package bb1;

import av.PersistenceConfigurationService;
import bc1.ClientSessionSubscriptionsLocalPersistence;
import be1.ClientSessionSubscriptionsLocalMemoryPersistence;
import bi1.ClientSessionSubscriptionsLocalXodusPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Provider;

public class ClientSessionSubscriptionsLocalPersistenceProvider
        implements Provider<ClientSessionSubscriptionsLocalPersistence> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientSessionSubscriptionsLocalPersistenceProvider.class);
    private final PersistenceConfigurationService persistenceConfigurationService;
    private final Provider<ClientSessionSubscriptionsLocalXodusPersistence> exodusPersistenceProvider;
    private final Provider<ClientSessionSubscriptionsLocalMemoryPersistence> memoryPersistenceProvider;

    @Inject
    ClientSessionSubscriptionsLocalPersistenceProvider(
            PersistenceConfigurationService persistenceConfigurationService,
            Provider<ClientSessionSubscriptionsLocalXodusPersistence> exodusPersistenceProvider,
            Provider<ClientSessionSubscriptionsLocalMemoryPersistence> memoryPersistenceProvider) {
        this.persistenceConfigurationService = persistenceConfigurationService;
        this.exodusPersistenceProvider = exodusPersistenceProvider;
        this.memoryPersistenceProvider = memoryPersistenceProvider;
    }

    @Override
    public ClientSessionSubscriptionsLocalPersistence get() {
        if (this.persistenceConfigurationService.getClientSessionSubscriptionMode() == PersistenceConfigurationService.PersistenceMode.IN_MEMORY) {
            LOGGER.trace("Using in-memory ClientSession Subscription store");
            return this.memoryPersistenceProvider.get();
        }
        LOGGER.trace("Using file based ClientSession Subscription store");
        return this.exodusPersistenceProvider.get();
    }
}
