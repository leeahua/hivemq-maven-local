package bm1;

import bu.InternalPublish;
import com.google.common.util.concurrent.ListenableFuture;
import com.hivemq.spi.annotations.NotNull;
import com.hivemq.spi.annotations.ThreadSafe;

@ThreadSafe
public interface QueuedMessagesSinglePersistence {
    ListenableFuture<Void> offer(@NotNull String clientId, @NotNull InternalPublish publish);

    ListenableFuture<Boolean> queuePublishIfQueueNotEmpty(@NotNull String clientId, @NotNull InternalPublish publish);

    ListenableFuture<InternalPublish> poll(@NotNull String clientId);
}
    
