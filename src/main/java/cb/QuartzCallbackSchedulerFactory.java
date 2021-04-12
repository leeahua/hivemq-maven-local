package cb;

import com.google.common.annotations.VisibleForTesting;
import com.hivemq.spi.exceptions.UnrecoverableException;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Provider;
import java.util.Properties;

public class QuartzCallbackSchedulerFactory implements Provider<SchedulerFactory> {
    private static final Logger LOGGER = LoggerFactory.getLogger(QuartzCallbackSchedulerFactory.class);
    static final String INSTANCE_NAME = "HiveMQ Callback Scheduler";
    public static final String INSTANCE_ID = "In Memory Callback Scheduler";

    @Override
    public SchedulerFactory get() {
        try {
            Properties config = getConfig();
            return new StdSchedulerFactory(config);
        } catch (SchedulerException e) {
            LOGGER.error("Could not create the Scheduler factory!", e);
            throw new UnrecoverableException(false);
        }
    }

    @VisibleForTesting
    protected Properties getConfig() {
        Properties config = new Properties();
        config.setProperty("org.quartz.scheduler.skipUpdateCheck", "true");
        config.setProperty("org.quartz.scheduler.instanceName", INSTANCE_NAME);
        config.setProperty("org.quartz.scheduler.instanceId", INSTANCE_ID);
        config.setProperty("org.quartz.threadPool.threadCount", "2");
        return config;
    }


}
