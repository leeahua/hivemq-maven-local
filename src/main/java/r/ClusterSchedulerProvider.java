package r;

import ap.ShutdownRegistry;
import av.HiveMQConfigurationService;
import av.Internals;
import cb1.ExtendedScheduledExecutorService;
import com.codahale.metrics.MetricRegistry;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import d.CacheScoped;
import i.ClusterIdProducer;
import s.Cluster;

import javax.inject.Inject;
import javax.inject.Provider;

@CacheScoped
public class ClusterSchedulerProvider implements Provider<ListeningScheduledExecutorService> {
    private final HiveMQConfigurationService hiveMQConfigurationService;
    private final MetricRegistry metricRegistry;
    private final ShutdownRegistry shutdownRegistry;
    private final ClusterIdProducer clusterIdProducer;
    private ExtendedScheduledExecutorService scheduledExecutorService;

    @Inject
    public ClusterSchedulerProvider(HiveMQConfigurationService hiveMQConfigurationService,
                                    MetricRegistry metricRegistry,
                                    ShutdownRegistry shutdownRegistry,
                                    ClusterIdProducer clusterIdProducer) {
        this.hiveMQConfigurationService = hiveMQConfigurationService;
        this.metricRegistry = metricRegistry;
        this.shutdownRegistry = shutdownRegistry;
        this.clusterIdProducer = clusterIdProducer;
    }

    @CacheScoped
    @Cluster
    @Override
    public ListeningScheduledExecutorService get() {
        if (this.scheduledExecutorService == null) {
            this.scheduledExecutorService = new ExtendedScheduledExecutorService(
                    this.hiveMQConfigurationService,
                    this.metricRegistry,
                    this.shutdownRegistry,
                    "cluster-scheduler",
                    Internals.CLUSTER_THREAD_POOL_SCHEDULED_SIZE,
                    "com.hivemq.cluster.executor.scheduled",
                    "cluster-scheduler-%d," + this.clusterIdProducer.get());
        }
        return this.scheduledExecutorService;
    }
}
