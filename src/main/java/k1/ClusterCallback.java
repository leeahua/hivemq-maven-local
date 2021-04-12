package k1;

import com.google.common.util.concurrent.SettableFuture;
import j1.ClusterRequest;
import p.ClusterRequestFailedException;

public abstract class ClusterCallback<S, Q extends ClusterRequest>
        extends AbstractClusterCallback<S, Q> {
    protected final SettableFuture<S> settableFuture = SettableFuture.create();

    public SettableFuture<S> getSettableFuture() {
        return settableFuture;
    }

    public void onFailed() {
        this.settableFuture.setException(new ClusterRequestFailedException("Request: " + this.request.getClass() + ", Receiver: " + this.receiver + "."));
    }

    public void onFailure(Throwable t) {
        this.settableFuture.setException(t);
    }
}
