package ao1;

import com.codahale.metrics.Gauge;
import io.netty.channel.group.ChannelGroup;

public class ConnectionsOverallCurrent implements Gauge<Integer> {
    private final ChannelGroup channelGroup;

    public ConnectionsOverallCurrent(ChannelGroup channelGroup) {
        this.channelGroup = channelGroup;
    }

    @Override
    public Integer getValue() {
        return this.channelGroup.size();
    }
}
