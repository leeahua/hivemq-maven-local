package ar1;

import am1.Metrics;
import an1.MetricsTypeListener;
import ao1.ClientSessionsCurrent;
import ao1.ConnectionsOverallCurrent;
import ao1.RetainedMessagesCurrent;
import ap1.GlobalTrafficCounter;
import as1.ClientSessionsCurrentProvider;
import as1.ConnectionsOverallCurrentProvider;
import as1.GlobalTrafficCounterProvider;
import as1.MetricsProvider;
import as1.RetainedMessagesCurrentProvider;
import c.BaseModule;
import com.codahale.metrics.MetricRegistry;
import com.google.inject.matcher.Matchers;
import g.NettyModule;

public class MetricModule extends BaseModule<MetricModule> {
    private final MetricRegistry metricRegistry;

    public MetricModule(MetricRegistry metricRegistry) {
        super(MetricModule.class);
        this.metricRegistry = metricRegistry;
    }

    protected void configure() {
        install(new NettyModule());
        bind(MetricRegistry.class).toInstance(this.metricRegistry);
        bind(Metrics.class).toProvider(MetricsProvider.class).asEagerSingleton();
        bind(ClientSessionsCurrent.class).toProvider(ClientSessionsCurrentProvider.class).asEagerSingleton();
        bind(GlobalTrafficCounter.class).toProvider(GlobalTrafficCounterProvider.class).asEagerSingleton();
        bind(ConnectionsOverallCurrent.class).toProvider(ConnectionsOverallCurrentProvider.class).asEagerSingleton();
        bind(RetainedMessagesCurrent.class).toProvider(RetainedMessagesCurrentProvider.class).asEagerSingleton();
        bindListener(Matchers.any(), new MetricsTypeListener(this.metricRegistry));
    }
}
