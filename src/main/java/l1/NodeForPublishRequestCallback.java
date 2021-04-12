package l1;

import com.google.common.util.concurrent.SettableFuture;
import com.hivemq.spi.annotations.Nullable;
import k1.ClusterCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import p.ClusterRequestException;
import t1.NodeForPublishRequest;
import v.ClientSessionClusterPersistence;

import java.util.concurrent.ExecutorService;

public class NodeForPublishRequestCallback
        extends ClusterCallback<String, NodeForPublishRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(NodeForPublishRequestCallback.class);
    private final ClientSessionClusterPersistence persistence;

    public NodeForPublishRequestCallback(ClientSessionClusterPersistence persistence) {
        this.persistence = persistence;
    }

    public void onSuccess(@Nullable String result) {
        this.settableFuture.set(result);
    }

    public void onDenied() {
        this.settableFuture.setException(new ClusterRequestException());
    }

    public void onNotResponsible() {
        this.settableFuture.setFuture(this.persistence.nodeForPublish(this.request.getClientId(), this.executorService));
    }

    public void onSuspected() {
        retry(new a(this.settableFuture, this.request, this.persistence, this.executorService));
    }

    public void onTimedOut() {
        this.LOGGER.trace("Node for publish request timed out.");
        retryAndIncreaseTimeout(String.class);
    }

    public void onBusy() {
        retry(String.class);
    }

    private static class a
            implements Runnable {
        private final SettableFuture<String> settableFuture;
        private final NodeForPublishRequest request;
        private final ClientSessionClusterPersistence persistence;
        private final ExecutorService executorService;

        a(SettableFuture<String> settableFuture,
          NodeForPublishRequest request,
          ClientSessionClusterPersistence persistence,
          ExecutorService executorService) {
            this.settableFuture = settableFuture;
            this.request = request;
            this.persistence = persistence;
            this.executorService = executorService;
        }

        public void run() {
            this.settableFuture.setFuture(
                    this.persistence.nodeForPublish(this.request.getClientId(), this.executorService));
        }
    }
}
