package q1;

import com.google.common.collect.ImmutableMap;
import com.hivemq.spi.annotations.Nullable;
import k1.ClusterCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import p.ClusterRequestException;
import y1.ClusterContextRequest;

public class ClusterContextRequestCallback
        extends ClusterCallback<ImmutableMap, ClusterContextRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterContextRequestCallback.class);

    public void onSuccess(@Nullable ImmutableMap result) {
        this.settableFuture.set(result);
    }

    public void onDenied() {
        throw new ClusterRequestException();
    }

    public void onNotResponsible() {
        throw new ClusterRequestException();
    }

    public void onSuspected() {
        retry(ImmutableMap.class);
    }

    public void onTimedOut() {
        LOGGER.trace("Cluster context request timed out.");
        retryAndIncreaseTimeout(ImmutableMap.class);
    }

    public void onBusy() {
        retry(ImmutableMap.class);
    }
}
