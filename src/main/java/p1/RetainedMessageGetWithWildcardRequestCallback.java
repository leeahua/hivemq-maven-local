package p1;

import k1.ClusterCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import p.ClusterRequestException;
import x1.RetainedMessageGetRequest;

import java.util.Set;

public class RetainedMessageGetWithWildcardRequestCallback
        extends ClusterCallback<Set, RetainedMessageGetRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(RetainedMessageGetWithWildcardRequestCallback.class);

    public void onSuccess(Set result) {
        this.settableFuture.set(result);
    }

    public void onDenied() {
        this.settableFuture.setException(new ClusterRequestException("Response was 'DENIED'."));
    }

    public void onNotResponsible() {
        this.settableFuture.setException(new ClusterRequestException("Response was 'NOT_RESPONSIBLE'."));
    }

    public void onSuspected() {
        retry(Set.class);
    }

    public void onTimedOut() {
        LOGGER.trace("Retained message get with wildcard request timeout.");
        retryAndIncreaseTimeout(Set.class);
    }

    public void onBusy() {
        retry(Set.class);
    }
}
