package bm1;

import d.CacheScoped;
import i.ClusterConfigurationService;
import y.ClientSessionSubscriptionsClusterPersistence;

import javax.inject.Inject;
import javax.inject.Provider;

@CacheScoped
public class ClientSessionSubscriptionClusterPersistenceProvider
        implements Provider<ClientSessionSubscriptionsClusterPersistence> {
    private final ClusterConfigurationService clusterConfigurationService;
    private final Provider<ClientSessionSubscriptionsSinglePersistenceImpl> singlePersistence;
    private final Provider<ClientSessionSubscriptionsClusterPersistenceImpl> clusterPersistence;

    @Inject
    public ClientSessionSubscriptionClusterPersistenceProvider(
            ClusterConfigurationService clusterConfigurationService,
            Provider<ClientSessionSubscriptionsSinglePersistenceImpl> singlePersistence,
            Provider<ClientSessionSubscriptionsClusterPersistenceImpl> clusterPersistence) {
        this.clusterConfigurationService = clusterConfigurationService;
        this.singlePersistence = singlePersistence;
        this.clusterPersistence = clusterPersistence;
    }

    public ClientSessionSubscriptionsClusterPersistence get() {
        if (this.clusterConfigurationService.isEnabled()) {
            return this.clusterPersistence.get();
        }
        return this.singlePersistence.get();
    }
}
