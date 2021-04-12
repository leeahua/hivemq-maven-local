package i;

import ah.ClusterService;
import ap.ShutdownRegistry;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.Inject;
import d.CacheScoped;
import org.jgroups.blocks.MessageDispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import t.ClusterConnection;
import t.ClusterReceiverService;

@CacheScoped
public class ClusterJoiner {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterJoiner.class);
    private final ClusterConnection clusterConnection;
    private final ClusterReceiverService clusterReceiverService;
    private final ClusterService clusterService;
    private final ShutdownRegistry shutdownRegistry;

    @Inject
    public ClusterJoiner(ClusterConnection clusterConnection,
                         ClusterReceiverService clusterReceiverService,
                         ClusterService clusterService,
                         ShutdownRegistry shutdownRegistry) {
        this.clusterConnection = clusterConnection;
        this.clusterReceiverService = clusterReceiverService;
        this.clusterService = clusterService;
        this.shutdownRegistry = shutdownRegistry;
    }

    public ListenableFuture<Void> join() {
        MessageDispatcher dispatcher = new MessageDispatcher(this.clusterConnection.getJChannel(),
                this.clusterReceiverService,
                this.clusterReceiverService,
                this.clusterReceiverService);
        dispatcher.asyncDispatching(true);
        try {
            this.clusterConnection.connect(this.clusterReceiverService, dispatcher);
        } catch (Exception e) {
            return Futures.immediateFailedCheckedFuture(e);
        }
        this.shutdownRegistry.register(new ClusterShutdown(dispatcher, this.clusterConnection));
        return this.clusterService.start();
    }
}
