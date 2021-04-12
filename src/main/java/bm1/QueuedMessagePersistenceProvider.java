package bm1;

import com.google.inject.Inject;
import com.google.inject.Provider;
import d.CacheScoped;
import i.ClusterConfigurationService;

@CacheScoped
public class QueuedMessagePersistenceProvider implements Provider<QueuedMessagesSinglePersistence> {
    private final ClusterConfigurationService clusterConfigurationService;
    private final Provider<QueuedMessagesSinglePersistenceImpl> singlePersistence;
    private final Provider<QueuedMessagesClusterPersistenceImpl> clusterPersistence;

    @Inject
    public QueuedMessagePersistenceProvider(
            ClusterConfigurationService clusterConfigurationService,
            Provider<QueuedMessagesSinglePersistenceImpl> singlePersistence,
            Provider<QueuedMessagesClusterPersistenceImpl> clusterPersistence) {
        this.clusterConfigurationService = clusterConfigurationService;
        this.singlePersistence = singlePersistence;
        this.clusterPersistence = clusterPersistence;
    }

    @Override
    public QueuedMessagesSinglePersistence get() {
        if (this.clusterConfigurationService.isEnabled()) {
            return this.clusterPersistence.get();
        }
        return this.singlePersistence.get();
    }
}
