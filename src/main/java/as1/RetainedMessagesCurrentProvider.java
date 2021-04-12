package as1;

import ao1.RetainedMessagesCurrent;
import com.codahale.metrics.MetricRegistry;
import com.hivemq.spi.metrics.HiveMQMetrics;
import x.RetainedMessagesSinglePersistence;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

public class RetainedMessagesCurrentProvider
        implements Provider<RetainedMessagesCurrent> {
    private final RetainedMessagesSinglePersistence retainedMessagesSinglePersistence;
    private final MetricRegistry metricRegistry;

    @Inject
    public RetainedMessagesCurrentProvider(RetainedMessagesSinglePersistence retainedMessagesSinglePersistence,
                                           MetricRegistry metricRegistry) {
        this.retainedMessagesSinglePersistence = retainedMessagesSinglePersistence;
        this.metricRegistry = metricRegistry;
    }

    @Singleton
    @Override
    public RetainedMessagesCurrent get() {
        RetainedMessagesCurrent retainedMessagesCurrent = new RetainedMessagesCurrent(this.retainedMessagesSinglePersistence);
        this.metricRegistry.register(HiveMQMetrics.RETAINED_MESSAGES_CURRENT.name(), retainedMessagesCurrent);
        return retainedMessagesCurrent;
    }
}
