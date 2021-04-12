package aw;

import com.hivemq.spi.services.configuration.GeneralConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;

@Singleton
public class GeneralConfigurationServiceImpl implements GeneralConfigurationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(GeneralConfigurationServiceImpl.class);
    private boolean updateCheckEnabled = true;

    public boolean updateCheckEnabled() {
        return this.updateCheckEnabled;
    }

    public void setUpdateCheckEnabled(boolean updateCheckEnabled) {
        if (updateCheckEnabled) {
            LOGGER.debug("Enabling the update check");
        } else {
            LOGGER.debug("Disabling the update check");
        }
        this.updateCheckEnabled = updateCheckEnabled;
    }
}
