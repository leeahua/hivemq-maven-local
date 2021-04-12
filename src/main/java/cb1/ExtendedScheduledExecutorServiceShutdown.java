package cb1;

import ap.Shutdown;
import com.hivemq.spi.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class ExtendedScheduledExecutorServiceShutdown extends Shutdown {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExtendedScheduledExecutorServiceShutdown.class);
    private final ExtendedScheduledExecutorService executorService;

    public ExtendedScheduledExecutorServiceShutdown(ExtendedScheduledExecutorService executorService) {
        this.executorService = executorService;
    }

    @NotNull
    public String name() {
        return "ExtendedScheduledExecutorService " + this.executorService.getExecutorName() + " shutdown";
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
            LOGGER.trace("not able to wait for ExtendedScheduledExecutorService shutdown", e);
        }
    }
}
