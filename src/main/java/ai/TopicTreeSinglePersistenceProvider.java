package ai;

import by.TopicTreeSinglePersistence;
import d.CacheScoped;
import i.ClusterConfigurationService;

import javax.inject.Inject;
import javax.inject.Provider;

@CacheScoped
public class TopicTreeSinglePersistenceProvider
        implements Provider<TopicTreeSinglePersistence> {
    private final ClusterConfigurationService clusterConfigurationService;
    private final Provider<TopicTreeSinglePersistenceImpl> singlePersistence;
    private final Provider<TopicTreeClusterPersistenceImpl> clusterPersistence;

    @Inject
    public TopicTreeSinglePersistenceProvider(
            ClusterConfigurationService clusterConfigurationService,
            Provider<TopicTreeSinglePersistenceImpl> singlePersistence,
            Provider<TopicTreeClusterPersistenceImpl> clusterPersistence) {
        this.clusterConfigurationService = clusterConfigurationService;
        this.singlePersistence = singlePersistence;
        this.clusterPersistence = clusterPersistence;
    }

    public TopicTreeSinglePersistence get() {
        if (this.clusterConfigurationService.isEnabled()) {
            return this.clusterPersistence.get();
        }
        return this.singlePersistence.get();
    }
}
