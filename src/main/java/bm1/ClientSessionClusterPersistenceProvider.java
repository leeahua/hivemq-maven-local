package bm1;

import i.ClusterConfigurationService;

import javax.inject.Inject;
import javax.inject.Provider;
import v.ClientSessionClusterPersistence;

public class ClientSessionClusterPersistenceProvider
        implements Provider<ClientSessionClusterPersistence> {
    private final ClusterConfigurationService clusterConfigurationService;
    private final Provider<ClientSessionSinglePersistenceImpl> singlePersistence;
    private final Provider<ClientSessionClusterPersistenceImpl> clusterPersistence;

    @Inject
    public ClientSessionClusterPersistenceProvider(
            ClusterConfigurationService clusterConfigurationService,
            Provider<ClientSessionSinglePersistenceImpl> singlePersistence,
            Provider<ClientSessionClusterPersistenceImpl> clusterPersistence) {
        this.clusterConfigurationService = clusterConfigurationService;
        this.singlePersistence = singlePersistence;
        this.clusterPersistence = clusterPersistence;
    }

    @Override
    public ClientSessionClusterPersistence get() {
        if (this.clusterConfigurationService.isEnabled()) {
            return this.clusterPersistence.get();
        }
        return this.singlePersistence.get();
    }
}
