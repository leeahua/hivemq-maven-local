package bo;

import aj.ClusterFutures;
import bm1.QueuedMessagesSinglePersistence;
import bu.InternalPublish;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;

public class DeliveryMessageToClientFailedTask implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(DeliveryMessageToClientFailedTask.class);
    private static final int MAX_PUBLISH_PAYLOAD = 1048576;
    private final QueuedMessagesSinglePersistence queuedMessagesSinglePersistence;
    private final String clientId;
    private final InternalPublish publish;
    private final SettableFuture<SendStatus> settableFuture;

    public DeliveryMessageToClientFailedTask(
            QueuedMessagesSinglePersistence queuedMessagesSinglePersistence,
            String clientId,
            InternalPublish publish,
            SettableFuture<SendStatus> settableFuture) {
        this.queuedMessagesSinglePersistence = queuedMessagesSinglePersistence;
        this.clientId = clientId;
        this.publish = publish;
        this.settableFuture = settableFuture;
    }

    public void run() {
        LOGGER.trace("Delivery of a message to client {} failed, queuing the message", this.clientId);
        if (this.publish.getPayload().length > 7340032) {
            return;
        }
        ListenableFuture<Void> future = this.queuedMessagesSinglePersistence.offer(this.clientId, publish.of(this.publish));
        ClusterFutures.addCallback(future, new FutureCallback<Void>() {

            @Override
            public void onSuccess(@Nullable Void result) {
                LOGGER.trace("Queued Publish for client {}", clientId);
                settableFuture.set(SendStatus.QUEUED);
            }

            @Override
            public void onFailure(Throwable t) {
                settableFuture.setException(t);
            }
        });
    }
}
