package cb;

import ap.Shutdown;
import ap.ShutdownRegistry;
import cd.QuartzJobFactory;
import com.hivemq.spi.annotations.NotNull;
import com.hivemq.spi.exceptions.UnrecoverableException;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Provider;

public class QuartzCallbackSchedulerProvider implements Provider<Scheduler> {
    private static final Logger LOGGER = LoggerFactory.getLogger(QuartzCallbackSchedulerProvider.class);
    private final Provider<SchedulerFactory> schedulerFactoryProvider;
    private final Provider<QuartzJobFactory> jobFactoryProvider;
    private final ShutdownRegistry shutdownRegistry;

    @Inject
    public QuartzCallbackSchedulerProvider(Provider<SchedulerFactory> schedulerFactoryProvider,
                                           Provider<QuartzJobFactory> jobFactoryProvider,
                                           ShutdownRegistry shutdownRegistry) {
        this.schedulerFactoryProvider = schedulerFactoryProvider;
        this.jobFactoryProvider = jobFactoryProvider;
        this.shutdownRegistry = shutdownRegistry;
    }

    @Override
    public Scheduler get() {
        try {
            Scheduler scheduler = this.schedulerFactoryProvider.get().getScheduler();
            scheduler.setJobFactory(this.jobFactoryProvider.get());
            registerShutdown(scheduler);
            return scheduler;
        } catch (SchedulerException e) {
            LOGGER.error("Could not start the Scheduler", e);
            throw new UnrecoverableException(false);
        }
    }

    private void registerShutdown(Scheduler scheduler) {
        this.shutdownRegistry.register(new QuartzSchedulerShutdown(scheduler));
    }


    public static class QuartzSchedulerShutdown extends Shutdown {
        private final Scheduler scheduler;

        public QuartzSchedulerShutdown(Scheduler scheduler) {
            this.scheduler = scheduler;
        }

        @NotNull
        public String name() {
            return "Quartz Scheduler Shutdown Job";
        }

        @NotNull
        public Priority priority() {
            return Priority.MEDIUM;
        }

        public boolean isAsync() {
            return true;
        }

        public void run() {
            try {
                this.scheduler.shutdown();
            } catch (SchedulerException e) {
                LOGGER.warn("Could not shutdown the scheduler for scheduled callbacks");
                LOGGER.debug("Original Exception", e);
            }
        }
    }
}
