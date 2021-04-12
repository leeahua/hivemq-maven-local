package cf;

import ap.Shutdown;
import ca.CallbackExecutor;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.hivemq.spi.annotations.NotNull;
import com.hivemq.spi.callback.cluster.ClusterDiscoveryCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

public class PluginClusterDiscoveryShutdown extends Shutdown {
    private static final Logger LOGGER = LoggerFactory.getLogger(PluginClusterDiscoveryShutdown.class);
    private final CallbackExecutor callbackExecutor;
    private final ClusterDiscoveryCallback callback;
    protected boolean hasBeenShutdown = false;

    public PluginClusterDiscoveryShutdown(CallbackExecutor callbackExecutor,
                                          ClusterDiscoveryCallback callback) {
        this.callbackExecutor = callbackExecutor;
        this.callback = callback;
    }

    @NotNull
    public String name() {
        return "Plugin cluster discovery";
    }

    @NotNull
    public Priority priority() {
        return Priority.FIRST;
    }

    public boolean isAsync() {
        return false;
    }

    public void run() {
        if (this.hasBeenShutdown) {
            return;
        }
        try {
            ListeningExecutorService executorService = MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor());
            ListenableFuture future = executorService.submit(() -> {
                try {
                    Thread.currentThread().setContextClassLoader(callbackExecutor.getClass().getClassLoader());
                    callback.destroy();
                } catch (Exception e) {
                    LOGGER.error("Uncaught exception in plugin", e);
                }
            });
            future.get();
            executorService.shutdownNow();
        } catch (InterruptedException e) {
            LOGGER.debug("Waiting for discovery plugin destroy interrupted");
        } catch (RejectedExecutionException e) {
            LOGGER.debug("Discovery plugin destroy execution rejected");
        } catch (ExecutionException e) {
            LOGGER.error("Error at discovery plugin destroy: {}", e.getMessage());
            LOGGER.debug("Original exception", e);
        }
        this.hasBeenShutdown = true;
    }
}
