package cd;

import com.google.common.base.Preconditions;
import com.hivemq.spi.annotations.NotNull;
import com.hivemq.spi.callback.Callback;
import com.hivemq.spi.callback.schedule.ScheduledCallback;
import d.CacheScoped;
import org.quartz.CronExpression;
import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

@CacheScoped
public class QuartzCallbackScheduler {
    private static final Logger LOGGER = LoggerFactory.getLogger(QuartzCallbackScheduler.class);
    static final String CALLBACK = "callback";
    private final Scheduler scheduler;

    @Inject
    public QuartzCallbackScheduler(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    public synchronized void schedule(@NotNull ScheduledCallback callback) {
        Preconditions.checkNotNull(callback, "A callback must not be null in order to get scheduled");
        startScheduler();
        try {
            CronTrigger trigger = createTrigger(callback);
            JobDetail job = createJob(callback);
            this.scheduler.scheduleJob(job, trigger);
        } catch (ParseException e) {
            throw new RuntimeException(String.format("Could not schedule Callback %s because the Cron Expression %s is invalid", new Object[]{callback.getClass(), callback.cronExpression()}), e);
        } catch (SchedulerException e) {
            LOGGER.error("Could not schedule Callback {}", callback.getClass(), e);
        }
    }

    public synchronized void unschedule(@NotNull ScheduledCallback callback) {
        Preconditions.checkNotNull(callback, "A callback must not be null in order to get unscheduled");
        try {
            this.scheduler.unscheduleJob(new TriggerKey(createTriggerIdentity(callback)));
        } catch (SchedulerException e) {
            LOGGER.error("Could not unschedule Callback {}", callback.getClass(), e);
        }
    }

    private CronTrigger createTrigger(ScheduledCallback callback) throws ParseException {
        CronExpression expression = new CronExpression(callback.cronExpression());
        CronTrigger trigger = TriggerBuilder.newTrigger()
                .withSchedule(CronScheduleBuilder.cronSchedule(expression))
                .withIdentity(createTriggerIdentity(callback))
                .build();
        return trigger;
    }

    private JobDetail createJob(ScheduledCallback callback) {
        Map<String, Callback> jobData = new HashMap<>();
        jobData.put(CALLBACK, callback);
        JobDetail job = JobBuilder.newJob(ScheduledCallbackJob.class)
                .withIdentity(callback.getClass().getName() + "_job")
                .setJobData(new JobDataMap(jobData))
                .build();
        return job;
    }

    private String createTriggerIdentity(ScheduledCallback callback) {
        return callback.getClass().getName() + "_trigger";
    }

    private void startScheduler() {
        try {
            if (!this.scheduler.isStarted()) {
                this.scheduler.start();
            }
        } catch (SchedulerException e) {
            throw new RuntimeException("Could not start Quartz Scheduler");
        }
    }

    public static class ScheduledCallbackJob implements Job {
        public void execute(JobExecutionContext context) {
            ScheduledCallback callback = (ScheduledCallback) context.getJobDetail().getJobDataMap().get(CALLBACK);
            callback.execute();
        }
    }
}
