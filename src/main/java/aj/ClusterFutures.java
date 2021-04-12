package aj;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.SettableFuture;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import s.Cluster;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;

public class ClusterFutures {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterFutures.class);
    @Inject
    @Cluster
    private static ListeningExecutorService clusterExecutor;

    public static ListenableFuture<Void> merge(ListenableFuture<Void> future1,
                                               ListenableFuture<Void> future2) {
        List<ListenableFuture<Void>> futures = new ArrayList<>();
        futures.add(future1);
        futures.add(future2);
        return merge(futures);
    }

    public static ListenableFuture<Void> merge(List<ListenableFuture<Void>> futures) {
        return merge(futures, clusterExecutor);
    }

    public static ListenableFuture<Void> merge(List<ListenableFuture<Void>> futures, ExecutorService executorService) {
        ListenableFuture<List<Void>> future = Futures.allAsList(futures);
        return Futures.transformAsync(future,
                input -> Futures.immediateFuture(null), executorService);
    }

    public static ListenableFuture<Void> merge(Set<ListenableFuture<Void>> futures) {
        return merge(futures, clusterExecutor);
    }

    public static ListenableFuture<Void> merge(Set<ListenableFuture<Void>> futures, ExecutorService executorService) {
        ListenableFuture<List<Void>> future = Futures.allAsList(futures);
        return Futures.transformAsync(future,
                input -> null, executorService);
    }

    public static <T> void setFuture(ListenableFuture<T> future, SettableFuture<T> settableFuture) {
        setFuture(future, settableFuture, clusterExecutor);
    }

    public static <T> void setFuture(ListenableFuture<T> future, SettableFuture<T> settableFuture, ExecutorService executorService) {
        Futures.addCallback(future, new FutureCallback<T>() {

            @Override
            public void onSuccess(@Nullable T result) {
                settableFuture.set(result);
            }

            @Override
            public void onFailure(Throwable t) {
                settableFuture.setException(t);
            }
        }, executorService);
    }

    public static <T> ListenableFuture<T> setFuture(ListenableFuture<ListenableFuture<T>> future) {
        SettableFuture<T> settableFuture = SettableFuture.create();
        Futures.addCallback(future, new FutureCallback<ListenableFuture<T>>() {

            @Override
            public void onSuccess(@Nullable ListenableFuture<T> result) {
                if (result == null) {
                    settableFuture.set(null);
                    return;
                }
                ClusterFutures.setFuture(result, settableFuture);
            }

            @Override
            public void onFailure(Throwable t) {
                settableFuture.setException(t);
            }
        });
        return settableFuture;
    }

    public static <T> void setFuture(ListenableFuture<Void> future, SettableFuture<T> settableFuture, T value) {
        setFuture(future, settableFuture, value, clusterExecutor);
    }

    public static <T> void setFuture(ListenableFuture<Void> future, SettableFuture<T> settableFuture, T value, ExecutorService executorService) {
        Futures.addCallback(future, new FutureCallback<Void>() {
            @Override
            public void onSuccess(@Nullable Void result) {
                settableFuture.set(value);
            }

            @Override
            public void onFailure(Throwable t) {
                settableFuture.setException(t);
            }
        }, executorService);
    }

    public static void waitFuture(ListenableFuture<?> future) {
        waitFuture(future, clusterExecutor);
    }

    public static void waitFuture(ListenableFuture<?> future, ExecutorService executorService) {
        Futures.addCallback(future, new FutureCallback<Object>() {
            @Override
            public void onSuccess(@Nullable Object result) {

            }

            @Override
            public void onFailure(Throwable t) {
                LOGGER.error("Uncaught exception", t);
            }
        }, executorService);
    }

    public static <T> void addCallback(ListenableFuture<T> future, FutureCallback<? super T> futureCallback) {
        Futures.addCallback(future, futureCallback, clusterExecutor);
    }
}
