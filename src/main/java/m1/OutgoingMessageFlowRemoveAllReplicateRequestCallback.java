package m1;

import bn1.OutgoingMessageFlowClusterPersistence;
import com.google.common.util.concurrent.SettableFuture;
import com.hivemq.spi.annotations.Nullable;
import k1.ClusterCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import u1.OutgoingMessageFlowRemoveAllReplicateRequest;

public class OutgoingMessageFlowRemoveAllReplicateRequestCallback
        extends ClusterCallback<Void, OutgoingMessageFlowRemoveAllReplicateRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(OutgoingMessageFlowRemoveAllReplicateRequestCallback.class);
    private final OutgoingMessageFlowClusterPersistence persistence;

    public OutgoingMessageFlowRemoveAllReplicateRequestCallback(
            OutgoingMessageFlowClusterPersistence persistence) {
        this.persistence = persistence;
    }

    public void onSuccess(@Nullable Void result) {
        this.settableFuture.set(result);
    }

    public void onDenied() {
        this.settableFuture.setFuture(this.persistence.replicateRemoveAll(this.request.getClientId(), this.request.getVectorClock()));
    }

    public void onNotResponsible() {
        this.settableFuture.setFuture(this.persistence.replicateRemoveAll(this.request.getClientId(), this.request.getVectorClock()));
    }

    public void onSuspected() {
        retry(new Task(this.settableFuture, this.request, this.persistence));
    }

    public void onTimedOut() {
        LOGGER.trace("Outgoing message flow remove all replication request timed out");
        retryAndIncreaseTimeout(Void.class);
    }

    public void onBusy() {
        retry(Void.class);
    }


    private static class Task
            implements Runnable {
        private final SettableFuture<Void> settableFuture;
        private final OutgoingMessageFlowRemoveAllReplicateRequest request;
        private final OutgoingMessageFlowClusterPersistence persistence;

        Task(SettableFuture<Void> settableFuture,
             OutgoingMessageFlowRemoveAllReplicateRequest request,
             OutgoingMessageFlowClusterPersistence persistence) {
            this.settableFuture = settableFuture;
            this.request = request;
            this.persistence = persistence;
        }

        public void run() {
            this.settableFuture.setFuture(this.persistence.replicateRemoveAll(
                    this.request.getClientId(), this.request.getVectorClock()));
        }
    }
}
