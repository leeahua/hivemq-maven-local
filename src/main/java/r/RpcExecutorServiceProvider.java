package r;

import ap.ShutdownRegistry;
import av.HiveMQConfigurationService;
import av.Internals;
import cb1.ExtendedExecutorService;
import com.codahale.metrics.MetricRegistry;
import com.google.common.util.concurrent.ListeningExecutorService;
import d.CacheScoped;
import i.ClusterIdProducer;
import s.Rpc;

import javax.inject.Inject;
import javax.inject.Provider;

@CacheScoped
public class RpcExecutorServiceProvider implements Provider<ListeningExecutorService> {
    private final HiveMQConfigurationService hiveMQConfigurationService;
    private final MetricRegistry metricRegistry;
    private final ShutdownRegistry shutdownRegistry;
    private final ClusterIdProducer clusterIdProducer;

    @Inject
    public RpcExecutorServiceProvider(HiveMQConfigurationService hiveMQConfigurationService,
                                      MetricRegistry metricRegistry,
                                      ShutdownRegistry shutdownRegistry,
                                      ClusterIdProducer clusterIdProducer) {
        this.hiveMQConfigurationService = hiveMQConfigurationService;
        this.metricRegistry = metricRegistry;
        this.shutdownRegistry = shutdownRegistry;
        this.clusterIdProducer = clusterIdProducer;
    }

    @CacheScoped
    @Rpc
    @Override
    public ListeningExecutorService get() {
        return new ExtendedExecutorService(this.hiveMQConfigurationService,
                this.metricRegistry,
                this.shutdownRegistry,
                "cluster-request-eventbus",
                Internals.CLUSTER_REQUEST_EVENTBUS_THREAD_POOL_KEEP_ALIVE_SECONDS,
                Internals.CLUSTER_REQUEST_EVENTBUS_THREAD_POOL_SIZE_MINIMUM,
                Internals.CLUSTER_REQUEST_EVENTBUS_THREAD_POOL_SIZE_MAXIMUM,
                "com.hivemq.cluster.request-eventbus.executor",
                "cluster-request-eventbus-%d," + this.clusterIdProducer.get());
    }
}
