package i;

import ap.Shutdown;
import com.hivemq.spi.annotations.NotNull;
import org.jgroups.blocks.MessageDispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import t.ClusterConnection;

public class ClusterShutdown extends Shutdown {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterShutdown.class);
    private final MessageDispatcher messageDispatcher;
    private final ClusterConnection clusterConnection;

    public ClusterShutdown(MessageDispatcher messageDispatcher, ClusterConnection clusterConnection) {
        this.messageDispatcher = messageDispatcher;
        this.clusterConnection = clusterConnection;
    }

    @NotNull
    public String name() {
        return "Cluster shutdown";
    }

    @NotNull
    public Priority priority() {
        return Priority.VERY_HIGH;
    }

    public boolean isAsync() {
        return false;
    }

    public void run() {
        LOGGER.debug("Running cluster shutdown hooks");
        this.clusterConnection.disconnect();
    }
}
