package d1;

import a1.ClusterReceiver;
import ab.ClusterResponse;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.hivemq.spi.annotations.NotNull;
import d.CacheScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import v1.MessageQueueDrainedAckRequest;
import w.QueuedMessagesClusterPersistence;

@CacheScoped
public class MessageQueueDrainedAckRequestReceiver
        implements ClusterReceiver<MessageQueueDrainedAckRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(MessageQueueDrainedAckRequestReceiver.class);
    private final QueuedMessagesClusterPersistence queuedMessagesClusterPersistence;

    @Inject
    public MessageQueueDrainedAckRequestReceiver(QueuedMessagesClusterPersistence queuedMessagesClusterPersistence) {
        this.queuedMessagesClusterPersistence = queuedMessagesClusterPersistence;
    }

    public void received(@NotNull MessageQueueDrainedAckRequest request, @NotNull ClusterResponse response, @NotNull String sender) {
        Preconditions.checkNotNull(request, "Request must not be null");
        Preconditions.checkNotNull(response, "Response must not be null");
        Preconditions.checkNotNull(sender, "Sender must not be null");
        LOGGER.trace("Received message queue drained ack.");
        this.queuedMessagesClusterPersistence.processAckDrained(request.getClientId(), sender);
        response.sendResult();
    }
}
