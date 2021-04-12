package ap1;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.Maps;
import com.hivemq.spi.metrics.HiveMQMetrics;
import io.netty.channel.ChannelHandler;
import io.netty.handler.traffic.GlobalTrafficShapingHandler;
import io.netty.handler.traffic.TrafficCounter;

import javax.annotation.PostConstruct;
import javax.inject.Singleton;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@ChannelHandler.Sharable
@Singleton
public class GlobalTrafficCounter extends GlobalTrafficShapingHandler {
    public static final int CHECK_INTERVAL_SECONDS = 1;
    private final MetricRegistry metricRegistry;

    public GlobalTrafficCounter(MetricRegistry metricRegistry,
                                ScheduledExecutorService scheduledExecutorService) {
        super(scheduledExecutorService, TimeUnit.SECONDS.toMillis(CHECK_INTERVAL_SECONDS));
        this.metricRegistry = metricRegistry;
    }

    @PostConstruct
    public void init() {
        createTrafficMetrics().entrySet().forEach(entry ->
                this.metricRegistry.register(entry.getKey(), entry.getValue())
        );
    }

    public Map<String, Gauge<Long>> createTrafficMetrics() {
        Map<String, Gauge<Long>> metrics = Maps.newHashMap();
        TrafficCounter trafficCounter = trafficCounter();
        metrics.put(HiveMQMetrics.BYTES_READ_CURRENT.name(), () -> trafficCounter.lastReadBytes());
        metrics.put(HiveMQMetrics.BYTES_WRITE_CURRENT.name(), () -> trafficCounter.lastWrittenBytes());
        metrics.put(HiveMQMetrics.BYTES_READ_TOTAL.name(), () -> trafficCounter.cumulativeReadBytes());
        metrics.put(HiveMQMetrics.BYTES_WRITE_TOTAL.name(), () -> trafficCounter.cumulativeWrittenBytes());
        return metrics;
    }
}
