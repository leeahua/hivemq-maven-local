package r1;

import ah.ClusterReplicationService;
import com.google.common.util.concurrent.SettableFuture;
import com.hivemq.spi.annotations.Nullable;
import k1.ClusterCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import p.ClusterRequestException;
import u.Filter;
import z1.ClientSessionSubscriptionAllReplicateRequest;

public class ClientSessionSubscriptionAllReplicateRequestCallback
        extends ClusterCallback<Void, ClientSessionSubscriptionAllReplicateRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientSessionSubscriptionAllReplicateRequestCallback.class);
    private final ClusterReplicationService clusterReplicationService;
    private final Filter filter;

    public ClientSessionSubscriptionAllReplicateRequestCallback(
            ClusterReplicationService clusterReplicationService, Filter filter) {
        this.clusterReplicationService = clusterReplicationService;
        this.filter = filter;
    }

    public void onSuccess(@Nullable Void result) {
        this.settableFuture.set(result);
    }

    public void onDenied() {
        this.settableFuture.setException(new ClusterRequestException("Unexpected Response DENIED for ClientSessionSubscriptionReplicateAll"));
    }

    public void onNotResponsible() {
        this.settableFuture.setException(new ClusterRequestException("Unexpected Response NOT_RESPONSIBLE for ClientSessionSubscriptionReplicateAll"));
    }

    public void onSuspected() {
        retry(new Task(this.settableFuture, this.clusterReplicationService, this.filter, this.receiver));
    }

    public void onTimedOut() {
        LOGGER.trace("ClientSessionSubscriptionReplicateAll timeout");
        retryAndIncreaseTimeout(Void.class);
    }

    public void onBusy() {
        retry(Void.class);
    }

    private static class Task
            implements Runnable {
        private final SettableFuture<Void> settableFuture;
        private final ClusterReplicationService clusterReplicationService;
        private final Filter filter;
        private final String receiver;

        Task(SettableFuture<Void> settableFuture,
             ClusterReplicationService clusterReplicationService,
             Filter filter,
             String receiver) {
            this.settableFuture = settableFuture;
            this.clusterReplicationService = clusterReplicationService;
            this.filter = filter;
            this.receiver = receiver;
        }

        public void run() {
            this.settableFuture.setFuture(
                    this.clusterReplicationService.replicateClientSessionSubscriptions(this.receiver, this.filter));
        }
    }
}
