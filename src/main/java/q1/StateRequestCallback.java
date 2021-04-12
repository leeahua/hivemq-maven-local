package q1;

import ah.ClusterService;
import com.google.common.util.concurrent.SettableFuture;
import com.hivemq.spi.exceptions.UnrecoverableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import y1.ClusterStateRequest;
import k1.ClusterCallback;

public class StateRequestCallback extends ClusterCallback<Void, ClusterStateRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(StateRequestCallback.class);
    private final ClusterService clusterService;

    public StateRequestCallback(ClusterService clusterService) {
        this.clusterService = clusterService;
    }

    public void onSuccess(Void result) {
        this.settableFuture.set(result);
    }

    public void onDenied() {
        LOGGER.trace("State {} request denied", this.request.getState());
        switch (request.getState()){
            case UNKNOWN:
                this.settableFuture.setException(new UnrecoverableException(true));
            case NOT_JOINED:
            case JOINING:
            case RUNNING:
            case MERGE_MINORITY:
                retry(new Task(this.settableFuture, this.request, this.clusterService));
        }
    }

    public void onNotResponsible() {
        LOGGER.trace("Not responsible for state {} request", this.request.getState());
        retry(new Task(this.settableFuture, this.request, this.clusterService));
    }

    public void onSuspected() {
        LOGGER.trace("State {} request, but suspected", this.request.getState());
        retry(new Task(this.settableFuture, this.request, this.clusterService));
    }

    public void onTimedOut() {
        LOGGER.trace("State request timeout.");
        retry(new Task(this.settableFuture, this.request, this.clusterService));
    }

    public void onBusy() {
        retry(Void.class);
    }

    private static class Task implements Runnable {
        private final SettableFuture<Void> settableFuture;
        private final ClusterStateRequest request;
        private final ClusterService clusterService;

        Task(SettableFuture<Void> settableFuture, ClusterStateRequest request, ClusterService clusterService) {
            this.settableFuture = settableFuture;
            this.request = request;
            this.clusterService = clusterService;
        }

        public void run() {
            this.settableFuture.setFuture(this.clusterService.sendStateToCoordinator(this.request.getState()));
        }
    }
}
