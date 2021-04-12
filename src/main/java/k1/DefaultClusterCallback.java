package k1;

import com.hivemq.spi.annotations.Nullable;
import j1.ClusterRequest;
import p.ClusterRequestException;

public class DefaultClusterCallback<S, Q extends ClusterRequest> extends ClusterCallback<S, Q> {
    private final Class<S> resultType;

    public DefaultClusterCallback(Class<S> resultType) {
        this.resultType = resultType;
    }

    public void onSuccess(@Nullable S result) {
        this.settableFuture.set(result);
    }

    public void onDenied() {
        this.settableFuture.setException(new ClusterRequestException("Response was 'DENIED'."));
    }

    public void onNotResponsible() {
        this.settableFuture.setException(new ClusterRequestException("Response was 'NOT RESPONSIBLE'."));
    }

    public void onSuspected() {
        retry(this.resultType);
    }

    public void onTimedOut() {
        retryAndIncreaseTimeout(this.resultType);
    }

    public void onBusy() {
        retry(this.resultType);
    }
}
