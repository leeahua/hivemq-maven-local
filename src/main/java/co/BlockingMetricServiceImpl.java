package co;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.hivemq.spi.annotations.Nullable;
import com.hivemq.spi.metrics.HiveMQMetric;
import com.hivemq.spi.services.AsyncMetricService;
import com.hivemq.spi.services.BlockingMetricService;
import com.hivemq.spi.services.MetricService;
import d.CacheScoped;

import javax.inject.Inject;
import java.util.Map;
import java.util.SortedMap;

@CacheScoped
public class BlockingMetricServiceImpl
        implements BlockingMetricService, MetricService {
    private final MetricRegistry metricRegistry;
    private final AsyncMetricService asyncMetricService;

    @Inject
    public BlockingMetricServiceImpl(MetricRegistry metricRegistry,
                                     AsyncMetricService asyncMetricService) {
        this.metricRegistry = metricRegistry;
        this.asyncMetricService = asyncMetricService;
    }

    @Nullable
    public <T extends Metric> T getHiveMQMetric(HiveMQMetric<T> metric) {
        String name = metric.name();
        Class<? extends Metric> clazz = metric.getClazz();
        SortedMap metrics;
        if (clazz.isAssignableFrom(Histogram.class)) {
            metrics = this.metricRegistry.getHistograms();
            if (metrics.containsKey(name)) {
                return (T) metrics.get(name);
            }
        } else if (clazz.isAssignableFrom(Gauge.class)) {
            metrics = this.metricRegistry.getGauges();
            if (metrics.containsKey(name)) {
                return (T) metrics.get(name);
            }
        } else if (clazz.isAssignableFrom(Timer.class)) {
            metrics = this.metricRegistry.getTimers();
            if (metrics.containsKey(name)) {
                return (T) metrics.get(name);
            }
        } else if (clazz.isAssignableFrom(Counter.class)) {
            metrics = this.metricRegistry.getCounters();
            if (metrics.containsKey(name)) {
                return (T) metrics.get(name);
            }
        } else if (clazz.isAssignableFrom(Meter.class)) {
            metrics = this.metricRegistry.getMeters();
            if (metrics.containsKey(name)) {
                return (T) metrics.get(name);
            }
        }
        return null;
    }

    @Nullable
    public <T extends Metric> Map<String, T> getClusterMetric(HiveMQMetric<T> metric) {
        try {
            return this.asyncMetricService.getClusterMetric(metric).get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public MetricRegistry getMetricRegistry() {
        return this.metricRegistry;
    }
}
