package ak1;

import ap.Shutdown;

import javax.inject.Inject;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class LifecycleShutdown extends Shutdown {
    private final PreDestroyRegistry preDestroyRegistry;

    @Inject
    LifecycleShutdown(PreDestroyRegistry preDestroyRegistry) {
        this.preDestroyRegistry = preDestroyRegistry;
    }

    public String name() {
        return "Lifecycle Shutdown";
    }

    public Priority priority() {
        return Priority.HIGH;
    }

    public boolean isAsync() {
        return false;
    }

    public void run() {
        try {
            this.preDestroyRegistry.destroy().get(5L, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        }
    }
}
