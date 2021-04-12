package bm1;

import com.google.common.util.concurrent.ListenableFuture;
import com.hivemq.spi.annotations.NotNull;
import com.hivemq.spi.annotations.ReadOnly;
import com.hivemq.spi.message.Topic;
import y.ClientSubscriptions;

public interface ClientSessionSubscriptionsSinglePersistence {
    ListenableFuture<Void> addSubscription(@NotNull String clientId, @NotNull Topic topic);

    @ReadOnly
    ListenableFuture<ClientSubscriptions> getSubscriptions(@NotNull String clientId);

    ListenableFuture<Void> removeSubscription(@NotNull String clientId, @NotNull Topic topic);
}
