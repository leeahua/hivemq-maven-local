package as1;

import ao1.ConnectionsOverallCurrent;
import com.codahale.metrics.MetricRegistry;
import com.hivemq.spi.metrics.HiveMQMetrics;
import io.netty.channel.group.ChannelGroup;

import javax.inject.Inject;
import javax.inject.Provider;

public class ConnectionsOverallCurrentProvider
        implements Provider<ConnectionsOverallCurrent> {
    private final MetricRegistry metricRegistry;
    private final ChannelGroup channelGroup;

    @Inject
    public ConnectionsOverallCurrentProvider(MetricRegistry metricRegistry,
                                             ChannelGroup channelGroup) {
        this.metricRegistry = metricRegistry;
        this.channelGroup = channelGroup;
    }

    @Override
    public ConnectionsOverallCurrent get() {
        ConnectionsOverallCurrent connectionsOverallCurrent = new ConnectionsOverallCurrent(this.channelGroup);
        this.metricRegistry.register(HiveMQMetrics.CONNECTIONS_OVERALL_CURRENT.name(), connectionsOverallCurrent);
        return connectionsOverallCurrent;
    }
}
