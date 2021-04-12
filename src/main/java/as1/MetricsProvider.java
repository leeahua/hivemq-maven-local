package as1;

import am1.Metrics;
import com.codahale.metrics.MetricRegistry;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

@Singleton
public class MetricsProvider implements Provider<Metrics> {
    private final MetricRegistry metricRegistry;

    @Inject
    public MetricsProvider(MetricRegistry metricRegistry) {
        this.metricRegistry = metricRegistry;
    }

    @Singleton
    public Metrics get() {
        return new Metrics(this.metricRegistry);
    }
}
