package f1;

import a1.ClusterReceiver;
import ab.ClusterResponse;
import ab.ClusterResponseCode;
import aj.ClusterFutures;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.Inject;
import com.hivemq.spi.annotations.NotNull;
import d.CacheScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import x.RetainedMessagesClusterPersistence;
import x1.RetainedMessageClearRequest;

import javax.annotation.Nullable;

@CacheScoped
public class RetainedMessageClearRequestReceiver
        implements ClusterReceiver<RetainedMessageClearRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(RetainedMessageClearRequestReceiver.class);
    private final RetainedMessagesClusterPersistence retainedMessagesClusterPersistence;

    @Inject
    public RetainedMessageClearRequestReceiver(RetainedMessagesClusterPersistence retainedMessagesClusterPersistence) {
        this.retainedMessagesClusterPersistence = retainedMessagesClusterPersistence;
    }

    public void received(@NotNull RetainedMessageClearRequest request, @NotNull ClusterResponse response, @NotNull String sender) {
        Preconditions.checkNotNull(request, "Request must not be null");
        Preconditions.checkNotNull(response, "Response must not be null");
        Preconditions.checkNotNull(sender, "Sender must not be null");
        LOGGER.trace("Received retained message clear from {}.", sender);
        ListenableFuture<Void> future = this.retainedMessagesClusterPersistence.clearLocally();
        ClusterFutures.addCallback(future, new FutureCallback<Void>() {

            @Override
            public void onSuccess(@Nullable Void result) {
                response.sendResult();
            }

            @Override
            public void onFailure(Throwable t) {
                response.sendResult(ClusterResponseCode.FAILED);
            }
        });
    }
}
