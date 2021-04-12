package cw;

import ca1.Update;
import com.hivemq.spi.services.ConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class UpdateChecker {
    static final Logger LOGGER = LoggerFactory.getLogger(UpdateChecker.class);
    private final UpdateNotificationJob updateNotificationJob;
    private final ScheduledExecutorService executorService;
    private final ConfigurationService configurationService;

    @Inject
    UpdateChecker(UpdateNotificationJob updateNotificationJob,
                  @Update ScheduledExecutorService executorService,
                  ConfigurationService configurationService) {
        this.updateNotificationJob = updateNotificationJob;
        this.executorService = executorService;
        this.configurationService = configurationService;
    }

    public void start() {
        boolean enabled = this.configurationService.generalConfiguration().updateCheckEnabled();
        if (enabled) {
            LOGGER.debug("Update check is enabled");
            this.executorService.scheduleAtFixedRate(this.updateNotificationJob, 0L, 1L, TimeUnit.DAYS);
        } else {
            LOGGER.debug("Update check is disabled");
        }
    }
}
