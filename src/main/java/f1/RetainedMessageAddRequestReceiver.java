package f1;

import a1.ClusterReceiver;
import a1.GenericRequestReceiver;
import ab.ClusterResponse;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ListenableFuture;
import com.hivemq.spi.annotations.NotNull;
import d.CacheScoped;
import i.ClusterConfigurationService;
import i.ClusterIdProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import q.ConsistentHashingRing;
import s.Minority;
import s.Primary;
import s.Rpc;
import x.RetainedMessagesSinglePersistence;
import x1.RetainedMessageAddRequest;

import javax.inject.Inject;
import java.util.concurrent.ExecutorService;

@CacheScoped
public class RetainedMessageAddRequestReceiver
        extends GenericRequestReceiver
        implements ClusterReceiver<RetainedMessageAddRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(RetainedMessageAddRequestReceiver.class);
    private final RetainedMessagesSinglePersistence retainedMessagesSinglePersistence;

    @Inject
    RetainedMessageAddRequestReceiver(
            RetainedMessagesSinglePersistence retainedMessagesSinglePersistence,
            @Rpc ExecutorService rpcExecutor,
            @Primary ConsistentHashingRing primaryRing,
            @Minority ConsistentHashingRing minorityRing,
            ClusterIdProducer clusterIdProducer,
            ClusterConfigurationService clusterConfigurationService) {
        super(primaryRing, minorityRing, clusterIdProducer, clusterConfigurationService.getReplicates().getQueuedMessages().getReplicateCount(), rpcExecutor);
        this.retainedMessagesSinglePersistence = retainedMessagesSinglePersistence;
    }

    public void received(@NotNull RetainedMessageAddRequest request, @NotNull ClusterResponse response, @NotNull String sender) {
        Preconditions.checkNotNull(request, "Request must not be null");
        Preconditions.checkNotNull(response, "Response must not be null");
        Preconditions.checkNotNull(sender, "Sender must not be null");
        LOGGER.trace("Received retained message ADD request for topic {}", request.getTopic());
        if (!responsible(request.getKey(), response, Void.class)) {
            return;
        }
        ListenableFuture<Void> future = this.retainedMessagesSinglePersistence.addOrReplace(request.getTopic(), request.getRetainedMessage());
        sendResult(future, response);
    }

    protected void onFailure(Throwable t) {
        LOGGER.error("Exception while processing retained message add request.", t);
    }
}
