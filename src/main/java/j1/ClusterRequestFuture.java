package j1;

import aj.ClusterFutures;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import k1.AbstractClusterCallback;
import org.jgroups.SuspectedException;
import p.*;
import t.ClusterConnection;
import k1.ClusterCallback;

import javax.annotation.Nullable;
import java.util.concurrent.ExecutorService;

public class ClusterRequestFuture<S, Q extends ClusterRequest> {
    private final ListenableFuture<S> future;
    private final String node;
    private final ClusterConnection clusterConnection;
    private final Q request;
    private final long timeout;
    private final ListeningScheduledExecutorService scheduledExecutorService;

    public ClusterRequestFuture(ListenableFuture<S> future,
                                String node,
                                ClusterConnection clusterConnection,
                                Q request,
                                long timeout,
                                ListeningScheduledExecutorService scheduledExecutorService) {
        this.future = future;
        this.node = node;
        this.clusterConnection = clusterConnection;
        this.request = request;
        this.timeout = timeout;
        this.scheduledExecutorService = scheduledExecutorService;
    }

    public ListenableFuture<S> setCallback(ClusterCallback<S, Q> callback) {
        doSetCallback(callback, null);
        return callback.getSettableFuture();
    }

    public ListenableFuture<S> setCallback(ClusterCallback<S, Q> callback, ExecutorService executorService) {
        doSetCallback(callback, executorService);
        return callback.getSettableFuture();
    }

    public void setCallback(AbstractClusterCallback<S, Q> callback) {
        doSetCallback(callback, null);
    }

    public void setCallback(AbstractClusterCallback<S, Q> callback, ExecutorService executorService) {
        doSetCallback(callback, executorService);
    }

    private void doSetCallback(AbstractClusterCallback<S, Q> callback, ExecutorService executorService) {
        callback.init(this.node, this.clusterConnection, this.request, this.timeout, this.scheduledExecutorService);
        FutureCallback<S> futureCallback = new FutureCallback<S>(){

            @Override
            public void onSuccess(@Nullable S result) {
                callback.onSuccess(result);
            }

            @Override
            public void onFailure(Throwable t) {
                if (t instanceof ClusterResponseDeniedException) {
                    callback.onDenied();
                } else if (t instanceof ClusterNotResponsibleException) {
                    callback.onNotResponsible();
                } else if (t instanceof SuspectedException) {
                    callback.onSuspected();
                } else if (t instanceof ClusterTimeoutException) {
                    callback.onTimedOut();
                } else if (t instanceof ClusterBusyException) {
                    callback.onBusy();
                } else if (t instanceof ClusterRequestFailedException) {
                    callback.onFailed();
                } else if (t instanceof ChannelNotAvailableException || t instanceof IllegalStateException) {
                    if (callback instanceof ClusterCallback) {
                        ((ClusterCallback) callback).getSettableFuture().set(null);
                    }
                } else {
                    callback.onFailure(t);
                }
            }
        };
        if (executorService == null) {
            ClusterFutures.addCallback(this.future, futureCallback);
        } else {
            Futures.addCallback(this.future, futureCallback, executorService);
        }
    }
}
