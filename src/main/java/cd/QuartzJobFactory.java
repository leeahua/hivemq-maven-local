package cd;

import com.google.inject.Injector;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.spi.JobFactory;
import org.quartz.spi.TriggerFiredBundle;

import javax.inject.Inject;

public class QuartzJobFactory implements JobFactory {
    private final Injector injector;

    @Inject
    QuartzJobFactory(Injector injector) {
        this.injector = injector;
    }

    public Job newJob(TriggerFiredBundle bundle, Scheduler scheduler) {
        JobDetail job = bundle.getJobDetail();
        Class<? extends Job> jobClass = job.getJobClass();
        return this.injector.getInstance(jobClass);
    }
}
