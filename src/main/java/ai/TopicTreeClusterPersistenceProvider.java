package ai;

import by.TopicTreeClusterPersistence;
import d.CacheScoped;
import i.ClusterConfigurationService;

import javax.inject.Inject;
import javax.inject.Provider;

@CacheScoped
public class TopicTreeClusterPersistenceProvider
        implements Provider<TopicTreeClusterPersistence> {
    private final ClusterConfigurationService clusterConfigurationService;
    private final Provider<TopicTreeSinglePersistenceImpl> singlePersistence;
    private final Provider<TopicTreeClusterPersistenceImpl> clusterPersistence;

    @Inject
    public TopicTreeClusterPersistenceProvider(
            ClusterConfigurationService clusterConfigurationService,
             Provider<TopicTreeSinglePersistenceImpl> singlePersistence,
             Provider<TopicTreeClusterPersistenceImpl> clusterPersistence) {
        this.clusterConfigurationService = clusterConfigurationService;
        this.singlePersistence = singlePersistence;
        this.clusterPersistence = clusterPersistence;
    }

    public TopicTreeClusterPersistence get() {
        if (this.clusterConfigurationService.isEnabled()) {
            return this.clusterPersistence.get();
        }
        return this.singlePersistence.get();
    }
}
