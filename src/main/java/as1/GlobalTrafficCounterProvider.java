package as1;

import ap1.GlobalTrafficCounter;
import com.codahale.metrics.MetricRegistry;
import io.netty.channel.EventLoopGroup;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

public class GlobalTrafficCounterProvider implements Provider<GlobalTrafficCounter> {
    private final MetricRegistry metricRegistry;
    private final EventLoopGroup childEventLoop;

    @Inject
    public GlobalTrafficCounterProvider(MetricRegistry metricRegistry,
                                        @Named("ChildEventLoop") EventLoopGroup childEventLoop) {
        this.metricRegistry = metricRegistry;
        this.childEventLoop = childEventLoop;
    }

    @Singleton
    @Override
    public GlobalTrafficCounter get() {
        GlobalTrafficCounter globalTrafficCounter = new GlobalTrafficCounter(this.metricRegistry, this.childEventLoop);
        globalTrafficCounter.init();
        return globalTrafficCounter;
    }
}
