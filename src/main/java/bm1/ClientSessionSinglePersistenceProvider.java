package bm1;

import d.CacheScoped;
import i.ClusterConfigurationService;

import javax.inject.Inject;
import javax.inject.Provider;

@CacheScoped
public class ClientSessionSinglePersistenceProvider
        implements Provider<ClientSessionSinglePersistence> {
    private final ClusterConfigurationService clusterConfigurationService;
    private final Provider<ClientSessionSinglePersistenceImpl> singlePersistenceProvider;
    private final Provider<ClientSessionClusterPersistenceImpl> clusterPersistenceProvider;

    @Inject
    public ClientSessionSinglePersistenceProvider(ClusterConfigurationService clusterConfigurationService,
                                                  Provider<ClientSessionSinglePersistenceImpl> singlePersistenceProvider,
                                                  Provider<ClientSessionClusterPersistenceImpl> clusterPersistenceProvider) {
        this.clusterConfigurationService = clusterConfigurationService;
        this.singlePersistenceProvider = singlePersistenceProvider;
        this.clusterPersistenceProvider = clusterPersistenceProvider;
    }

    @Override
    public ClientSessionSinglePersistence get() {
        if (this.clusterConfigurationService.isEnabled()) {
            return this.clusterPersistenceProvider.get();
        }
        return this.singlePersistenceProvider.get();
    }
}
