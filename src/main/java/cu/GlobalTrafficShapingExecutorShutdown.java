package cu;

import ap.Shutdown;
import com.hivemq.spi.annotations.NotNull;

import java.util.concurrent.ScheduledExecutorService;

public class GlobalTrafficShapingExecutorShutdown extends Shutdown {
    private final ScheduledExecutorService globalTrafficShapingExecutorService;

    public GlobalTrafficShapingExecutorShutdown(ScheduledExecutorService globalTrafficShapingExecutorService) {
        this.globalTrafficShapingExecutorService = globalTrafficShapingExecutorService;
    }

    @NotNull
    public String name() {
        return "Global Traffic Shaping Executor Shutdown Hook";
    }

    @NotNull
    public Priority priority() {
        return Priority.HIGH;
    }

    public boolean isAsync() {
        return false;
    }

    public void run() {
        this.globalTrafficShapingExecutorService.shutdownNow();
    }
}
