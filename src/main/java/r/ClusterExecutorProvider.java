package r;

import ap.ShutdownRegistry;
import av.HiveMQConfigurationService;
import av.Internals;
import cb1.ExtendedExecutorService;
import com.codahale.metrics.MetricRegistry;
import com.google.common.util.concurrent.ListeningExecutorService;
import d.CacheScoped;
import i.ClusterIdProducer;
import s.Cluster;

import javax.inject.Inject;
import javax.inject.Provider;

@CacheScoped
public class ClusterExecutorProvider implements Provider<ListeningExecutorService> {
    private final HiveMQConfigurationService hiveMQConfigurationService;
    private final MetricRegistry metricRegistry;
    private final ShutdownRegistry shutdownRegistry;
    private final ClusterIdProducer clusterIdProducer;
    private ExtendedExecutorService executorService;

    @Inject
    public ClusterExecutorProvider(HiveMQConfigurationService hiveMQConfigurationService,
                                   MetricRegistry metricRegistry,
                                   ShutdownRegistry shutdownRegistry,
                                   ClusterIdProducer clusterIdProducer) {
        this.hiveMQConfigurationService = hiveMQConfigurationService;
        this.metricRegistry = metricRegistry;
        this.shutdownRegistry = shutdownRegistry;
        this.clusterIdProducer = clusterIdProducer;
    }

    @Cluster
    @Override
    public ListeningExecutorService get() {
        if (this.executorService == null) {
            this.executorService = new ExtendedExecutorService(this.hiveMQConfigurationService,
                    this.metricRegistry,
                    this.shutdownRegistry,
                    "cluster-executor",
                    Internals.CLUSTER_THREAD_POOL_KEEP_ALIVE_SECONDS,
                    Internals.CLUSTER_THREAD_POOL_SIZE_MINIMUM,
                    Internals.CLUSTER_THREAD_POOL_SIZE_MAXIMUM,
                    "com.hivemq.cluster.executor",
                    "cluster-executor-%d," + this.clusterIdProducer.get());
        }
        return this.executorService;
    }
}
