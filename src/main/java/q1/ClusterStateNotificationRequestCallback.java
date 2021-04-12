package q1;

import com.hivemq.spi.annotations.Nullable;
import k1.ClusterCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import y1.ClusterStateNotificationRequest;

public class ClusterStateNotificationRequestCallback
        extends ClusterCallback<Void, ClusterStateNotificationRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterStateNotificationRequestCallback.class);

    public void onSuccess(@Nullable Void result) {
        this.settableFuture.set(result);
    }

    public void onDenied() {
        retry(Void.class);
    }

    public void onNotResponsible() {
        retry(Void.class);
    }

    public void onSuspected() {
        retry(Void.class);
    }

    public void onTimedOut() {
        LOGGER.trace("State notification timeout.");
        retryAndIncreaseTimeout(Void.class);
    }

    public void onBusy() {
        retry(Void.class);
    }
}
