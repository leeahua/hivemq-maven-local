package p1;

import com.hivemq.spi.annotations.Nullable;
import k1.ClusterCallback;
import p.ClusterRequestException;
import x1.RetainedMessageClearRequest;

public class RetainedMessageClearRequestCallback
        extends ClusterCallback<Void, RetainedMessageClearRequest> {
    public void onSuccess(@Nullable Void result) {
        this.settableFuture.set(result);
    }

    public void onDenied() {
        this.settableFuture.setException(new ClusterRequestException("Response was 'DENIED'."));
    }

    public void onNotResponsible() {
        this.settableFuture.setException(new ClusterRequestException("Response was 'NOT RESPONSIBLE'."));
    }

    public void onSuspected() {
        retry(Void.class);
    }

    public void onTimedOut() {
        retryAndIncreaseTimeout(Void.class);
    }

    public void onBusy() {
        retry(Void.class);
    }
}
