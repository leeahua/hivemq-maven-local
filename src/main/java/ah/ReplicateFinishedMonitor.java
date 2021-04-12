package ah;

import aj.ClusterFutures;
import av.InternalConfigurationService;
import av.Internals;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import j1.ClusterRequestFuture;
import k1.ClusterCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import t.ClusterConnection;
import y1.ReplicateFinishedRequest;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class ReplicateFinishedMonitor {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReplicateFinishedMonitor.class);
    private final ClusterStateService clusterStateService;
    private final ClusterConnection clusterConnection;
    private final InternalConfigurationService internalConfigurationService;
    private final SettableFuture<Void> settableFuture = SettableFuture.create();
    private final Set<String> replicatingNodes = Collections.synchronizedSet(new HashSet<>());
    private final ScheduledExecutorService scheduledExecutorService;
    private ScheduledFuture<?> scheduledFuture;

    public ReplicateFinishedMonitor(ClusterStateService clusterStateService,
                                    ClusterConnection clusterConnection,
                                    InternalConfigurationService internalConfigurationService,
                                    ScheduledExecutorService scheduledExecutorService) {
        this.clusterStateService = clusterStateService;
        this.clusterConnection = clusterConnection;
        this.internalConfigurationService = internalConfigurationService;
        this.scheduledExecutorService = scheduledExecutorService;
    }

    public ListenableFuture<Void> start() {
        int retryInterval = this.internalConfigurationService.getInt(Internals.CLUSTER_RETRY_INTERVAL_REPLICATION_FINISHED);
        this.scheduledFuture = this.scheduledExecutorService.scheduleAtFixedRate(() -> {
            try {
                sendReplicationFinished();
            } catch (Exception e) {
                settableFuture.setException(e);
            }
        }, 0L, retryInterval, TimeUnit.MILLISECONDS);
        ClusterFutures.addCallback(this.settableFuture, new FutureCallback<Void>() {

            @Override
            public void onSuccess(@Nullable Void result) {
                scheduledFuture.cancel(true);
            }

            @Override
            public void onFailure(Throwable t) {
                scheduledFuture.cancel(true);
            }
        });
        return this.settableFuture;
    }

    public void sendReplicationFinished() {
        Set<String> nodes = this.clusterStateService.getNodes(ClusterState.RUNNING, ClusterState.SHUTTING_DOWN);
        nodes.removeAll(this.replicatingNodes);
        if (nodes.isEmpty()) {
            this.settableFuture.set(null);
            return;
        }
        nodes.forEach(node -> {
            ListenableFuture<Boolean> future = sendReplicationFinishedRequest(node);
            ClusterFutures.addCallback(future, new FutureCallback<Boolean>() {

                @Override
                public void onSuccess(@Nullable Boolean result) {
                    if (result) {
                        replicatingNodes.add(node);
                    }
                }

                @Override
                public void onFailure(Throwable t) {
                    settableFuture.setException(t);
                }
            });
        });
    }

    private ListenableFuture<Boolean> sendReplicationFinishedRequest(String node) {
        LOGGER.trace("Sending Replication finished request to {}.", node);
        ClusterRequestFuture requestFuture = this.clusterConnection.send(new ReplicateFinishedRequest(), node, Boolean.TYPE);
        return requestFuture.setCallback(new ReplicateFinishedRequestCallback());
    }

    private class ReplicateFinishedRequestCallback
            extends ClusterCallback<Boolean, ReplicateFinishedRequest> {
        private ReplicateFinishedRequestCallback() {
        }

        public void onSuccess(Boolean result) {
            this.settableFuture.set(result);
        }

        public void onDenied() {
            ReplicateFinishedMonitor.this.replicatingNodes.add(this.receiver);
        }

        public void onNotResponsible() {
            ReplicateFinishedMonitor.this.replicatingNodes.add(this.receiver);
        }

        public void onSuspected() {
            ReplicateFinishedMonitor.this.replicatingNodes.add(this.receiver);
        }

        public void onFailure(Throwable t) {
            this.settableFuture.setException(t);
        }

        public void onTimedOut() {
        }

        public void onBusy() {
        }
    }
}
