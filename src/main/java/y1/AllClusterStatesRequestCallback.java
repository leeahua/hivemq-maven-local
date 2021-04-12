package y1;

import ah.ClusterService;
import com.google.common.util.concurrent.SettableFuture;
import com.hivemq.spi.annotations.Nullable;
import k1.ClusterCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class AllClusterStatesRequestCallback
        extends ClusterCallback<Map, AllClusterStatesRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(AllClusterStatesRequestCallback.class);
    private final ClusterService clusterService;

    public AllClusterStatesRequestCallback(ClusterService clusterService) {
        this.clusterService = clusterService;
    }

    public void onSuccess(@Nullable Map result) {
        this.settableFuture.set(result);
    }

    public void onDenied() {
        this.settableFuture.setFuture(this.clusterService.getClusterStates());
    }

    public void onNotResponsible() {
        this.settableFuture.setFuture(this.clusterService.getClusterStates());
    }

    public void onSuspected() {
        retry(new Task(this.settableFuture, this.clusterService));
    }

    public void onTimedOut() {
        retryAndIncreaseTimeout(Map.class);
    }

    public void onBusy() {
        retry(Map.class);
    }

    private static class Task implements Runnable {
        private final SettableFuture<Map> settableFuture;
        private final ClusterService clusterService;

        Task(SettableFuture<Map> settableFuture, ClusterService clusterService) {
            this.settableFuture = settableFuture;
            this.clusterService = clusterService;
        }

        public void run() {
            this.settableFuture.setFuture(this.clusterService.getClusterStates());
        }
    }
}
