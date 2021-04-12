package bm1;

import com.google.inject.Inject;
import com.google.inject.Provider;
import i.ClusterConfigurationService;
import w.QueuedMessagesClusterPersistence;

public class QueuedMessagesClusterPersistenceProvider
        implements Provider<QueuedMessagesClusterPersistence> {
    private final ClusterConfigurationService clusterConfigurationService;
    private final Provider<QueuedMessagesSinglePersistenceImpl> singlePersistence;
    private final Provider<QueuedMessagesClusterPersistenceImpl> clusterPersistence;

    @Inject
    public QueuedMessagesClusterPersistenceProvider(
            ClusterConfigurationService clusterConfigurationService,
            Provider<QueuedMessagesSinglePersistenceImpl> singlePersistence,
            Provider<QueuedMessagesClusterPersistenceImpl> clusterPersistence) {
        this.clusterConfigurationService = clusterConfigurationService;
        this.singlePersistence = singlePersistence;
        this.clusterPersistence = clusterPersistence;
    }

    public QueuedMessagesClusterPersistence get() {
        if (this.clusterConfigurationService.isEnabled()) {
            return this.clusterPersistence.get();
        }
        return this.singlePersistence.get();
    }
}
