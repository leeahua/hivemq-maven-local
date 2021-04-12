package bm1;

import d.CacheScoped;
import i.ClusterConfigurationService;

import javax.inject.Inject;
import javax.inject.Provider;

@CacheScoped
public class ClientSessionSubscriptionsPersistenceProvider
        implements Provider<ClientSessionSubscriptionsSinglePersistence> {
    private final ClusterConfigurationService clusterConfigurationService;
    private final Provider<ClientSessionSubscriptionsSinglePersistenceImpl> singlePersistence;
    private final Provider<ClientSessionSubscriptionsClusterPersistenceImpl> clusterPersistence;

    @Inject
    public ClientSessionSubscriptionsPersistenceProvider(
            ClusterConfigurationService clusterConfigurationService,
            Provider<ClientSessionSubscriptionsSinglePersistenceImpl> singlePersistence,
            Provider<ClientSessionSubscriptionsClusterPersistenceImpl> clusterPersistence) {
        this.clusterConfigurationService = clusterConfigurationService;
        this.singlePersistence = singlePersistence;
        this.clusterPersistence = clusterPersistence;
    }

    @Override
    public ClientSessionSubscriptionsSinglePersistence get() {
        if (this.clusterConfigurationService.isEnabled()) {
            return this.clusterPersistence.get();
        }
        return this.singlePersistence.get();
    }
}
