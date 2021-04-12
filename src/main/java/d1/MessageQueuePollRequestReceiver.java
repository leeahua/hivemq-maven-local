package d1;

import a1.AbstractRequestReceiver;
import a1.ClusterReceiver;
import ab.ClusterResponse;
import ab.ClusterResponseCode;
import aj.ClusterFutures;
import bu.InternalPublish;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.Inject;
import com.hivemq.spi.annotations.NotNull;
import d.CacheScoped;
import i.ClusterConfigurationService;
import i.ClusterIdProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import q.ConsistentHashingRing;
import s.Minority;
import s.Primary;
import v1.MessageQueuePollRequest;
import w.QueuedMessagesClusterPersistence;

import javax.annotation.Nullable;

@CacheScoped
public class MessageQueuePollRequestReceiver
        extends AbstractRequestReceiver
        implements ClusterReceiver<MessageQueuePollRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(MessageQueuePollRequestReceiver.class);
    private final QueuedMessagesClusterPersistence queuedMessagesClusterPersistence;

    @Inject
    protected MessageQueuePollRequestReceiver(
            QueuedMessagesClusterPersistence queuedMessagesClusterPersistence,
            @Primary ConsistentHashingRing primaryRing,
            @Minority ConsistentHashingRing consistentHashingRing,
            ClusterIdProducer clusterIdProducer,
            ClusterConfigurationService clusterConfigurationService) {
        super(primaryRing, consistentHashingRing, clusterIdProducer, clusterConfigurationService.getReplicates().getClientSession().getReplicateCount());
        this.queuedMessagesClusterPersistence = queuedMessagesClusterPersistence;
    }

    public void received(@NotNull MessageQueuePollRequest request, @NotNull ClusterResponse response, @NotNull String sender) {
        Preconditions.checkNotNull(request, "Request must not be null");
        Preconditions.checkNotNull(response, "Response must not be null");
        Preconditions.checkNotNull(sender, "Sender must not be null");
        LOGGER.trace("Received message queue poll request for client {}.", request.getClientId());
        if (!responsible(request.getKey(), response, Void.class)) {
            return;
        }
        ListenableFuture<InternalPublish> future = this.queuedMessagesClusterPersistence.processPollRequest(
                request.getClientId(), request.getRequestId(), sender);
        ClusterFutures.addCallback(future, new FutureCallback<InternalPublish>(){

            @Override
            public void onSuccess(@Nullable InternalPublish result) {
                response.sendResult(result);
            }

            @Override
            public void onFailure(Throwable t) {
                LOGGER.error("Polling from message queue failed.", t);
                response.sendResult(ClusterResponseCode.FAILED);
            }
        });
    }
}
