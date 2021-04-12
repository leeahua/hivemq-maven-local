package x;

import d.CacheScoped;
import i.ClusterConfigurationService;

import javax.inject.Inject;
import javax.inject.Provider;

public class RetainedMessageClusterPersistenceProvider
        implements Provider<RetainedMessagesClusterPersistence> {
    private final ClusterConfigurationService clusterConfigurationService;
    private final Provider<RetainedMessagesSinglePersistenceImpl> singlePersistence;
    private final Provider<RetainedMessagesClusterPersistenceImpl> clusterPersistence;

    @Inject
    public RetainedMessageClusterPersistenceProvider(
            ClusterConfigurationService clusterConfigurationService,
            Provider<RetainedMessagesSinglePersistenceImpl> singlePersistence,
            Provider<RetainedMessagesClusterPersistenceImpl> clusterPersistence) {
        this.clusterConfigurationService = clusterConfigurationService;
        this.singlePersistence = singlePersistence;
        this.clusterPersistence = clusterPersistence;
    }

    @CacheScoped
    @Override
    public RetainedMessagesClusterPersistence get() {
        if (this.clusterConfigurationService.isEnabled()) {
            return this.clusterPersistence.get();
        }
        return this.singlePersistence.get();
    }
}
