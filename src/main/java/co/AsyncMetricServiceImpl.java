package co;

import aj.ClusterFutures;
import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.hivemq.spi.metrics.HiveMQMetric;
import com.hivemq.spi.services.AsyncMetricService;
import com.hivemq.spi.services.BlockingMetricService;
import d.CacheScoped;
import i.ClusterConfigurationService;
import k1.ClusterCallback;
import q1.MetricRequestCallback;
import t.ClusterCallbackFactory;
import t.ClusterConnection;
import y1.MetricRequest;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;

@CacheScoped
public class AsyncMetricServiceImpl implements AsyncMetricService {
    private final MetricRegistry metricRegistry;
    private final ClusterConfigurationService clusterConfigurationService;
    private final ClusterConnection clusterConnection;
    private final BlockingMetricService blockingMetricService;

    @Inject
    public AsyncMetricServiceImpl(MetricRegistry metricRegistry,
                                  ClusterConfigurationService clusterConfigurationService,
                                  ClusterConnection clusterConnection,
                                  BlockingMetricService blockingMetricService) {
        this.metricRegistry = metricRegistry;
        this.clusterConfigurationService = clusterConfigurationService;
        this.clusterConnection = clusterConnection;
        this.blockingMetricService = blockingMetricService;
    }

    public <T extends Metric> ListenableFuture<T> getHiveMQMetric(HiveMQMetric<T> metric) {
        return Futures.immediateFuture(this.blockingMetricService.getHiveMQMetric(metric));
    }

    public <T extends Metric> ListenableFuture<Map<String, T>> getClusterMetric(HiveMQMetric<T> metric) {
        String name = metric.name();
        Class<? extends Metric> clazz = metric.getClazz();
        SortedMap<String, ? extends Metric> metrics;
        if (clazz.isAssignableFrom(Histogram.class)) {
            metrics = this.metricRegistry.getHistograms();
            if (metrics.containsKey(name)) {
                return getMetric((T) metrics.get(name), name, clazz);
            }
        } else if (clazz.isAssignableFrom(Gauge.class)) {
            metrics = this.metricRegistry.getGauges();
            if (metrics.containsKey(name)) {
                return getMetric((T) metrics.get(name), name, clazz);
            }
        } else if (clazz.isAssignableFrom(Timer.class)) {
            metrics = this.metricRegistry.getTimers();
            if (metrics.containsKey(name)) {
                return getMetric((T) metrics.get(name), name, clazz);
            }
        } else if (clazz.isAssignableFrom(Counter.class)) {
            metrics = this.metricRegistry.getCounters();
            if (metrics.containsKey(name)) {
                return getMetric((T) metrics.get(name), name, clazz);
            }
        } else if (clazz.isAssignableFrom(Meter.class)) {
            metrics = this.metricRegistry.getMeters();
            if (metrics.containsKey(name)) {
                return getMetric((T) metrics.get(name), name, clazz);
            }
        }
        return Futures.immediateFuture(null);
    }

    public MetricRegistry getMetricRegistry() {
        return this.metricRegistry;
    }

    private <T extends Metric> ListenableFuture<Map<String, T>> getMetric(
            T metric, String name, Class<? extends Metric> type) {
        if (metric == null) {
            return Futures.immediateFuture(null);
        }
        Map<String, T> metrics = new HashMap<>();
        metrics.put(this.clusterConnection.getClusterId(), metric);
        if (!this.clusterConfigurationService.isEnabled()) {
            return Futures.immediateFuture(metrics);
        }
        MetricRequest request = new MetricRequest(name, type);
        ListenableFuture<Void> future = this.clusterConnection.send(
                request, type, new ClusterMetricCallbackFactory(type), true
                , (node, result) -> metrics.put(node, (T)result));
        SettableFuture<Map<String, T>> settableFuture = SettableFuture.create();
        ClusterFutures.setFuture(future, settableFuture, metrics);
        return settableFuture;
    }

    private static class ClusterMetricCallbackFactory<T> implements ClusterCallbackFactory<T, MetricRequest> {
        private final Class<T> type;

        private ClusterMetricCallbackFactory(Class<T> type) {
            this.type = type;
        }

        public ClusterCallback<T, MetricRequest> create() {
            return new MetricRequestCallback(this.type);
        }
    }
}
