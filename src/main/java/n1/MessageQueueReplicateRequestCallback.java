package n1;

import ah.ClusterReplicationService;
import com.google.common.util.concurrent.SettableFuture;
import com.hivemq.spi.annotations.Nullable;
import k1.ClusterCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import p.ClusterRequestException;
import u.Filter;
import v1.MessageQueueReplicateRequest;

public class MessageQueueReplicateRequestCallback
        extends ClusterCallback<Void, MessageQueueReplicateRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(MessageQueueReplicateRequestCallback.class);
    private final ClusterReplicationService replicationService;
    private final Filter filter;

    public MessageQueueReplicateRequestCallback(ClusterReplicationService replicationService,
                                                Filter filter) {
        this.replicationService = replicationService;
        this.filter = filter;
    }

    public void onSuccess(@Nullable Void result) {
        this.settableFuture.set(result);
    }

    public void onDenied() {
        this.settableFuture.setException(new ClusterRequestException("Unexpected Response DENIED for MessageQueueReplication"));
    }

    public void onNotResponsible() {
        this.settableFuture.setException(new ClusterRequestException("Unexpected Response NOT RESPONSIBLE for MessageQueueReplication"));
    }

    public void onSuspected() {
        retry(new Task(this.settableFuture, this.replicationService, this.filter, this.receiver));
    }

    public void onTimedOut() {
        LOGGER.trace("Message queue replication timed out.");
        retryAndIncreaseTimeout(Void.class);
    }

    public void onBusy() {
        retry(Void.class);
    }


    private static class Task
            implements Runnable {
        private final SettableFuture<Void> settableFuture;
        private final ClusterReplicationService replicationService;
        private final Filter filter;
        private final String receiver;

        Task(SettableFuture<Void> settableFuture,
             ClusterReplicationService replicationService,
             Filter filter,
             String receiver) {
            this.settableFuture = settableFuture;
            this.replicationService = replicationService;
            this.filter = filter;
            this.receiver = receiver;
        }

        public void run() {
            this.settableFuture.setFuture(this.replicationService.replicateQueuedMessages(this.receiver, this.filter));
        }
    }
}
