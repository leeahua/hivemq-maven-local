package bf1;

import av.PersistenceConfigurationService.QueuedMessagesStrategy;
import com.google.common.base.Preconditions;
import com.hivemq.spi.annotations.NotNull;

public class ClientSessionQueueFactory {

    public static ClientSessionQueue create(long maxQueuedMessages,
                                       @NotNull QueuedMessagesStrategy strategy,
                                       boolean clusterEnabled) {
        Preconditions.checkNotNull(strategy, "QueuedMessagesStrategy must not be null");
        Preconditions.checkArgument(maxQueuedMessages >= 0L, "size must be greater or equal 0");
        if (maxQueuedMessages == 0L) {
            return EmptyClientSessionQueue.INSTANCE;
        }
        if (strategy == QueuedMessagesStrategy.DISCARD) {
            return new DiscardClientSessionQueue(maxQueuedMessages);
        }
        if (clusterEnabled) {
            return new BufferedDiscardOldestClientSessionQueue(maxQueuedMessages);
        }
        return new DiscardOldestClientSessionQueue(maxQueuedMessages);
    }
}
