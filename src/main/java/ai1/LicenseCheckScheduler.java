package ai1;

import ag1.Licensing;
import com.hivemq.spi.exceptions.UnrecoverableException;
import org.quartz.DailyTimeIntervalScheduleBuilder;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.ScheduleBuilder;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.TimeOfDay;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.util.Date;
import java.util.HashMap;
import java.util.UUID;

public class LicenseCheckScheduler {
    private static final Logger LOGGER = LoggerFactory.getLogger(LicenseCheckScheduler.class);
    static final String RUNNABLE = "runnable";
    private final Scheduler scheduler;

    @Inject
    LicenseCheckScheduler(@Licensing Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    @PreDestroy
    public void destroy() {
        try {
            this.scheduler.shutdown();
        } catch (SchedulerException e) {
            LOGGER.warn("Could not shutdown the LicenseCheck scheduler");
            LOGGER.debug("Original Exception", e);
        }
    }

    public Date schedule(Runnable runnable) {
        startScheduler();
        Trigger trigger = createTrigger();
        JobDetail jobDetail = createJobDetail(runnable);
        try {
            return this.scheduler.scheduleJob(jobDetail, trigger);
        } catch (SchedulerException e) {
            LOGGER.error("Could not schedule daily license check request: ", e);
            throw new UnrecoverableException();
        }
    }

    private void startScheduler() {
        try {
            if (!this.scheduler.isStarted()) {
                this.scheduler.start();
            }
        } catch (SchedulerException e) {
            LOGGER.error("Could not start scheduler for daily license check", e);
            throw new UnrecoverableException();
        }
    }

    private JobDetail createJobDetail(Runnable runnable) {
        HashMap jobDataMap = new HashMap();
        jobDataMap.put(RUNNABLE, runnable);
        return JobBuilder.newJob(LicensingDailyJob.class)
                .withIdentity(runnable.getClass() + "licensing_daily_job" + UUID.randomUUID().toString())
                .setJobData(new JobDataMap(jobDataMap))
                .build();
    }

    private Trigger createTrigger() {
        ScheduleBuilder scheduleBuilder = createScheduleBuilder();
        return TriggerBuilder
                .newTrigger()
                .withSchedule(scheduleBuilder)
                .withIdentity("licensing_daily_trigger" + UUID.randomUUID().toString())
                .build();
    }

    private ScheduleBuilder createScheduleBuilder() {
        return DailyTimeIntervalScheduleBuilder
                .dailyTimeIntervalSchedule()
                .withIntervalInHours(24)
                .startingDailyAt(TimeOfDay.hourAndMinuteOfDay(0, 15))
                .onEveryDay()
                .withMisfireHandlingInstructionDoNothing();
    }

    public static class LicensingDailyJob implements Job {
        public void execute(JobExecutionContext context) {
            Runnable runnable = (Runnable) context.getJobDetail().getJobDataMap().get(RUNNABLE);
            runnable.run();
        }
    }
}
