package af1;

import ag1.Licensing;
import av.InternalConfigurationService;
import cd.QuartzJobFactory;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import d.CacheScoped;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

public class LicensingSchedulerModule extends AbstractModule {
    private static final Logger LOGGER = LoggerFactory.getLogger(LicensingSchedulerModule.class);

    protected void configure() {
    }


    @Provides
    @CacheScoped
    @Licensing
    public Scheduler provideLicensingScheduler(@Licensing SchedulerFactory schedulerFactory,
                                      QuartzJobFactory quartzJobFactory) throws SchedulerException {
        Scheduler scheduler = schedulerFactory.getScheduler();
        scheduler.setJobFactory(quartzJobFactory);
        return scheduler;
    }

    @Provides
    @CacheScoped
    @Licensing
    public SchedulerFactory provideLicensingSchedulerFactory(InternalConfigurationService internalConfigurationService) throws SchedulerException {
        Properties properties = new Properties();
        properties.setProperty("org.quartz.scheduler.skipUpdateCheck", "true");
        properties.setProperty("org.quartz.scheduler.instanceName", "Licensing Scheduler");
        properties.setProperty("org.quartz.scheduler.instanceId", "In Memory Licensing Scheduler");
        properties.setProperty("org.quartz.threadPool.threadCount", "2");
        return new StdSchedulerFactory(properties);
    }
}
