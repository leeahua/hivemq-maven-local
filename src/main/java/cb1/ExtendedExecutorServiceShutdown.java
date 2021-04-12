package cb1;

import ap.Shutdown;
import com.hivemq.spi.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class ExtendedExecutorServiceShutdown extends Shutdown {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExtendedExecutorServiceShutdown.class);
    private final ExtendedExecutorService executorService;

    public ExtendedExecutorServiceShutdown(ExtendedExecutorService executorService) {
        this.executorService = executorService;
    }

    @NotNull
    public String name() {
        return "ExtendedExecutorService " + this.executorService.getName() + " shutdown";
    }

    @NotNull
    public Priority priority() {
        return Priority.LOW;
    }

    public boolean isAsync() {
        return true;
    }

    public void run() {
        this.executorService.shutdownNow();
        try {
            this.executorService.awaitTermination(1L, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            LOGGER.trace("not able to wait for ExtendedExecutorService shutdown", e);
        }
    }
}
