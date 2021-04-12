package k1;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableScheduledFuture;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.hivemq.spi.annotations.NotNull;
import com.hivemq.spi.annotations.Nullable;
import j1.ClusterRequest;
import j1.ClusterRequestFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import t.ClusterConnection;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public abstract class AbstractClusterCallback<S, Q extends ClusterRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractClusterCallback.class);
    protected String receiver;
    protected ClusterConnection clusterConnection;
    protected Q request;
    protected long initialTimeout;
    protected ListeningScheduledExecutorService executorService;
    protected long timeout;
    private FutureCallback futureCallback;
    private final AtomicReference<ListenableScheduledFuture> future = new AtomicReference();

    public final void init(String receiver,
                        ClusterConnection clusterConnection,
                        Q request,
                        long initialTimeout,
                        ListeningScheduledExecutorService executorService) {
        this.receiver = receiver;
        this.clusterConnection = clusterConnection;
        this.request = request;
        this.initialTimeout = initialTimeout;
        this.executorService = executorService;
        this.timeout = initialTimeout + retryFactor(initialTimeout);
        this.futureCallback = createRetryFutureCallback(request);
    }

    private long retryFactor(long timeout) {
        return this.request.hashCode() % timeout;
    }

    public abstract void onSuccess(@Nullable S result);

    public abstract void onDenied();

    public abstract void onNotResponsible();

    public abstract void onSuspected();

    public abstract void onFailure(Throwable t);

    public abstract void onTimedOut();

    public abstract void onBusy();

    public abstract void onFailed();

    protected void retry(Runnable runnable) {
        this.executorService.schedule(runnable, this.initialTimeout, TimeUnit.MILLISECONDS);
    }

    protected void retry(Class<S> returnType) {
        if (this.future.compareAndSet(null, scheduleRetry(returnType))) {
            Futures.addCallback(this.future.get(), this.futureCallback, this.executorService);
        }
    }

    @NotNull
    private FutureCallback createRetryFutureCallback(Q request) {
        return new RetryFutureCallback(request, this.future);
    }

    @NotNull
    private ListenableScheduledFuture<?> scheduleRetry(Class<S> returnType) {
        return this.executorService.schedule(() -> retryAndIncreaseTimeout(returnType), this.timeout, TimeUnit.MILLISECONDS);
    }


    protected void retryAndIncreaseTimeout(Class<S> returnType) {
        if (this.timeout < this.initialTimeout * 20L) {
            this.timeout += this.initialTimeout / 2L + retryFactor(this.initialTimeout) / 2L;
        }
        ClusterRequestFuture<S, Q> future =
                this.clusterConnection.send(this.request, this.receiver, returnType, this.timeout);
        future.setCallback(this);
    }

    private static class RetryFutureCallback<Q> implements FutureCallback {
        private static final Logger LOGGER = LoggerFactory.getLogger(RetryFutureCallback.class);
        private final Q request;
        private final AtomicReference<ListenableScheduledFuture> future;

        public RetryFutureCallback(Q request,
                                   AtomicReference<ListenableScheduledFuture> future) {
            this.request = request;
            this.future = future;
        }

        public void onSuccess(@Nullable Object result) {
            this.future.set(null);
        }

        public void onFailure(Throwable t) {
            LOGGER.error("Exception during scheduled retry for {}.", this.request.getClass(), t);
            this.future.set(null);
        }
    }
}
