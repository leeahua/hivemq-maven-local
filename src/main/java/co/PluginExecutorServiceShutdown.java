package co;

import ap.Shutdown;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.hivemq.spi.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class PluginExecutorServiceShutdown extends Shutdown {
    private static final Logger LOGGER = LoggerFactory.getLogger(PluginExecutorServiceShutdown.class);
    private final ListeningScheduledExecutorService pluginExecutorService;

    public PluginExecutorServiceShutdown(ListeningScheduledExecutorService pluginExecutorService) {
        this.pluginExecutorService = pluginExecutorService;
    }

    @NotNull
    public String name() {
        return "PluginExecutorService shutdown";
    }

    @NotNull
    public Priority priority() {
        return Priority.LOW;
    }

    public boolean isAsync() {
        return true;
    }

    public void run() {
        this.pluginExecutorService.shutdownNow();
        try {
            this.pluginExecutorService.awaitTermination(1L, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            LOGGER.trace("not able to wait for PluginExecutorService shutdown", e);
        }
    }
}
