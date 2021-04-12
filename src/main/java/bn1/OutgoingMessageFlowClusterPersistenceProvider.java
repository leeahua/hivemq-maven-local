package bn1;

import d.CacheScoped;
import i.ClusterConfigurationService;

import javax.inject.Inject;
import javax.inject.Provider;

@CacheScoped
public class OutgoingMessageFlowClusterPersistenceProvider
        implements Provider<OutgoingMessageFlowClusterPersistence> {
    private final ClusterConfigurationService clusterConfigurationService;
    private final Provider<OutgoingMessageFlowSinglePersistenceImpl> singlePersistence;
    private final Provider<OutgoingMessageFlowClusterPersistenceImpl> clusterPersistence;

    @Inject
    public OutgoingMessageFlowClusterPersistenceProvider(
            ClusterConfigurationService clusterConfigurationService,
            Provider<OutgoingMessageFlowSinglePersistenceImpl> singlePersistence,
            Provider<OutgoingMessageFlowClusterPersistenceImpl> clusterPersistence) {
        this.clusterConfigurationService = clusterConfigurationService;
        this.singlePersistence = singlePersistence;
        this.clusterPersistence = clusterPersistence;
    }

    public OutgoingMessageFlowClusterPersistence get() {
        if (this.clusterConfigurationService.isEnabled()) {
            return this.clusterPersistence.get();
        }
        return this.singlePersistence.get();
    }
}
