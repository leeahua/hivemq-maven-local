package cv;

import ap.ShutdownRegistry;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.hivemq.spi.annotations.NotNull;
import com.hivemq.spi.services.configuration.ThrottlingConfigurationService;
import com.hivemq.spi.services.configuration.ValueChangedCallback;
import cu.GlobalTrafficShapingExecutorShutdown;
import io.netty.handler.traffic.GlobalTrafficShapingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

public class GlobalTrafficShapingHandlerProvider
        implements Provider<GlobalTrafficShapingHandler> {
    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalTrafficShapingHandlerProvider.class);
    private final ShutdownRegistry shutdownRegistry;
    private final ThrottlingConfigurationService throttlingConfigurationService;

    @Inject
    GlobalTrafficShapingHandlerProvider(ShutdownRegistry shutdownRegistry,
                                        ThrottlingConfigurationService throttlingConfigurationService) {
        this.shutdownRegistry = shutdownRegistry;
        this.throttlingConfigurationService = throttlingConfigurationService;
    }

    @Override
    public GlobalTrafficShapingHandler get() {
        ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("global-traffic-shaping-executor-%d").build();
        ScheduledExecutorService globalTrafficShapingExecutorService = Executors.newSingleThreadScheduledExecutor(threadFactory);
        GlobalTrafficShapingExecutorShutdown shutdown = new GlobalTrafficShapingExecutorShutdown(globalTrafficShapingExecutorService);
        this.shutdownRegistry.register(shutdown);
        long incomingLimit = this.throttlingConfigurationService.incomingLimit();
        LOGGER.debug("Throttling incoming traffic to {} B/s", incomingLimit);
        long outgoingLimit = this.throttlingConfigurationService.outgoingLimit();
        LOGGER.debug("Throttling outgoing traffic to {} B/s", outgoingLimit);
        GlobalTrafficShapingHandler handler = new GlobalTrafficShapingHandler(globalTrafficShapingExecutorService, outgoingLimit, incomingLimit, 1000L);
        ValueChangedCallback changedCallback = createValueChangedCallback(handler);
        this.throttlingConfigurationService.incomingLimitChanged(changedCallback);
        this.throttlingConfigurationService.outgoingLimitChanged(changedCallback);
        return handler;
    }


    @NotNull
    private ValueChangedCallback<Long> createValueChangedCallback(GlobalTrafficShapingHandler handler) {
        return new ThrottlingTrafficLimitValueChangedCallback(handler, this.throttlingConfigurationService);
    }

    private static class ThrottlingTrafficLimitValueChangedCallback
            implements ValueChangedCallback<Long> {
        private final GlobalTrafficShapingHandler globalTrafficShapingHandler;
        private final ThrottlingConfigurationService throttlingConfigurationService;

        public ThrottlingTrafficLimitValueChangedCallback(GlobalTrafficShapingHandler globalTrafficShapingHandler,
                                                          ThrottlingConfigurationService throttlingConfigurationService) {
            this.globalTrafficShapingHandler = globalTrafficShapingHandler;
            this.throttlingConfigurationService = throttlingConfigurationService;
        }

        @Override
        public void valueChanged(Long newValue) {
            this.globalTrafficShapingHandler.configure(
                    this.throttlingConfigurationService.outgoingLimit(),
                    this.throttlingConfigurationService.incomingLimit());
        }
    }
}
