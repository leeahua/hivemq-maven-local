package q1;

import com.hivemq.spi.annotations.Nullable;
import k1.ClusterCallback;
import p.ClusterRequestException;
import y1.NodeInformationRequest;

public class NodeInformationRequestCallback
        extends ClusterCallback<Void, NodeInformationRequest> {
    public void onSuccess(@Nullable Void result) {
        this.settableFuture.set(result);
    }

    public void onDenied() {
        this.settableFuture.setException(new ClusterRequestException("Response code was 'DENIED'."));
    }

    public void onNotResponsible() {
        this.settableFuture.setException(new ClusterRequestException("Response code was 'DENIED'."));
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
