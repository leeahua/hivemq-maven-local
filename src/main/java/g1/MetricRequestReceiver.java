package g1;

import a1.ClusterReceiver;
import ab.ClusterResponse;
import com.codahale.metrics.Metric;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.hivemq.spi.annotations.NotNull;
import com.hivemq.spi.metrics.HiveMQMetric;
import com.hivemq.spi.services.BlockingMetricService;
import d.CacheScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import y1.MetricRequest;

@CacheScoped
public class MetricRequestReceiver
        implements ClusterReceiver<MetricRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(MetricRequestReceiver.class);
    private final BlockingMetricService blockingMetricService;
    
    @Inject
    public MetricRequestReceiver(BlockingMetricService blockingMetricService) {
        this.blockingMetricService = blockingMetricService;
    }

    public void received(@NotNull MetricRequest request, @NotNull ClusterResponse response, @NotNull String sender) {
        Preconditions.checkNotNull(request, "Request must not be null");
        Preconditions.checkNotNull(response, "Response must not be null");
        Preconditions.checkNotNull(sender, "Sender must not be null");
        LOGGER.trace("Received metric request for metric {} from {}.", request.getMetricName(), sender);
        Metric metric = this.blockingMetricService.getHiveMQMetric(
                HiveMQMetric.valueOf(request.getMetricName(), request.getMetricClass()));
        response.sendResult(metric);
    }
}
