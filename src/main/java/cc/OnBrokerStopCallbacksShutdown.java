package cc;

import ap.Shutdown;
import com.hivemq.spi.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

public class OnBrokerStopCallbacksShutdown extends Shutdown {
    private static final Logger LOGGER = LoggerFactory.getLogger(OnBrokerStopCallbacksShutdown.class);
    static final String NAME = "OnBrokerStop Callbacks Shutdown Hook";
    private final PluginBrokerCallbackHandler handler;

    @Inject
    OnBrokerStopCallbacksShutdown(PluginBrokerCallbackHandler handler) {
        this.handler = handler;
    }

    @NotNull
    public String name() {
        return NAME;
    }

    @NotNull
    public Priority priority() {
        return Priority.LOW;
    }

    public boolean isAsync() {
        return true;
    }

    public void run() {
        long startMillis = System.currentTimeMillis();
        LOGGER.debug("Executing all plugin shutdown hooks");
        this.handler.onStop();
        long spendMillis = System.currentTimeMillis() - startMillis;
        LOGGER.debug("Finished all plugin shutdown hooks in {}s", spendMillis);
    }
}
