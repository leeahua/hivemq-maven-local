package q1;

import com.hivemq.spi.annotations.Nullable;
import k1.ClusterCallback;
import p.ClusterRequestException;
import y1.MetricRequest;

public class MetricRequestCallback<T> extends ClusterCallback<T, MetricRequest> {
    private final Class<T> resultType;

    public MetricRequestCallback(Class<T> resultType) {
        this.resultType = resultType;
    }

    public void onSuccess(@Nullable T result) {
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
