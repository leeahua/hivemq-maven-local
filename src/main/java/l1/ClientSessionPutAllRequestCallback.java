package l1;

import ah.ClusterReplicationService;
import com.google.common.util.concurrent.SettableFuture;
import com.hivemq.spi.annotations.Nullable;
import k1.ClusterCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import p.ClusterRequestException;
import t1.ClientSessionPutAllRequest;
import u.Filter;

public class ClientSessionPutAllRequestCallback
        extends ClusterCallback<Void, ClientSessionPutAllRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientSessionPutAllRequestCallback.class);
    private final ClusterReplicationService clusterReplicationService;
    private final Filter filter;

    public ClientSessionPutAllRequestCallback(
            ClusterReplicationService clusterReplicationService,
            Filter filter) {
        this.clusterReplicationService = clusterReplicationService;
        this.filter = filter;
    }

    public void onSuccess(@Nullable Void result) {
        this.settableFuture.set(result);
    }

    public void onDenied() {
        this.settableFuture.setException(new ClusterRequestException("Unexpected response DENIED for ClientSession replication"));
    }

    public void onNotResponsible() {
        this.settableFuture.setException(new ClusterRequestException("Unexpected response NOT_RESPONSIBLE for ClientSession replication"));
    }

    public void onSuspected() {
        retry(new Task(this.settableFuture, this.clusterReplicationService, this.receiver, this.filter));
    }

    public void onTimedOut() {
        this.LOGGER.trace("Client session put all request timed out.");
        retryAndIncreaseTimeout(Void.class);
    }

    public void onBusy() {
        retry(Void.class);
    }

    private static class Task
            implements Runnable {
        private final SettableFuture<Void> settableFuture;
        private final ClusterReplicationService clusterReplicationService;
        private final String receiver;
        private final Filter filter;

        Task(SettableFuture<Void> settableFuture,
             ClusterReplicationService clusterReplicationService,
             String receiver,
             Filter filter) {
            this.settableFuture = settableFuture;
            this.clusterReplicationService = clusterReplicationService;
            this.receiver = receiver;
            this.filter = filter;
        }

        public void run() {
            this.settableFuture.setFuture(
                    this.clusterReplicationService.replicateClientSession(this.receiver, this.filter));
        }
    }
}
