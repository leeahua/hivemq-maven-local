package bb1;

import av.PersistenceConfigurationService;
import bc1.QueuedMessagesLocalPersistence;
import be1.QueuedMessagesLocalMemoryPersistence;
import bj1.QueuedMessagesLocalFilePersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Provider;

public class QueuedMessagesLocalPersistenceProvider
        implements Provider<QueuedMessagesLocalPersistence> {
    private static final Logger LOGGER = LoggerFactory.getLogger(QueuedMessagesLocalPersistenceProvider.class);
    private final PersistenceConfigurationService persistenceConfigurationService;
    private final Provider<QueuedMessagesLocalFilePersistence> filePersistenceProvider;
    private final Provider<QueuedMessagesLocalMemoryPersistence> memoryPersistenceProvider;

    @Inject
    QueuedMessagesLocalPersistenceProvider(
            PersistenceConfigurationService persistenceConfigurationService,
            Provider<QueuedMessagesLocalFilePersistence> filePersistenceProvider,
            Provider<QueuedMessagesLocalMemoryPersistence> memoryPersistenceProvider) {
        this.persistenceConfigurationService = persistenceConfigurationService;
        this.filePersistenceProvider = filePersistenceProvider;
        this.memoryPersistenceProvider = memoryPersistenceProvider;
    }

    @Override
    public QueuedMessagesLocalPersistence get() {
        if (this.persistenceConfigurationService.getClientSessionQueuedMessagesMode() == PersistenceConfigurationService.PersistenceMode.IN_MEMORY) {
            LOGGER.trace("Using in-memory ClientSession Queued Messages store");
            return this.memoryPersistenceProvider.get();
        }
        LOGGER.trace("Using file based ClientSession Queued Messages store");
        return this.filePersistenceProvider.get();
    }
}
