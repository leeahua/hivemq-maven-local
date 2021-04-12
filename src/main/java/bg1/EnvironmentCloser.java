package bg1;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.hivemq.spi.annotations.NotNull;
import jetbrains.exodus.ExodusException;
import jetbrains.exodus.env.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EnvironmentCloser {
    private static final Logger LOGGER = LoggerFactory.getLogger(b.class);
    private final String name;
    private final Environment environment;
    private final int maxTries;
    private final int retryInterval;
    private int tries = 0;

    public EnvironmentCloser(@NotNull String name,
                             @NotNull Environment environment,
                             int maxTries,
                             int retryInterval) {
        Preconditions.checkNotNull(name, "Name must not be null");
        Preconditions.checkNotNull(environment, "Environment must not be null");
        Preconditions.checkArgument(maxTries > 0, "maxTries must be higher than 0. %s was provided", maxTries);
        Preconditions.checkArgument(retryInterval > 0, "retryInterval must be higher than 0. %s was provided", retryInterval);
        this.name = name;
        this.environment = environment;
        this.maxTries = maxTries;
        this.retryInterval = retryInterval;
    }


    public boolean close() {
        try {
            if (!this.environment.isOpen()) {
                LOGGER.warn("Tried to close store {} although it is already closed", this.name);
                return false;
            }
            if (this.tries < this.maxTries) {
                this.environment.close();
                return true;
            }
            LOGGER.error("Could not close store {} after {} tries.", this.name, this.tries);
            return false;
        } catch (ExodusException e) {
            return onError(e);
        }
    }

    private boolean onError(ExodusException cause) {
        if (!"Finish all transactions before closing database environment".equals(cause.getMessage())) {
            throw cause;
        }
        this.tries += 1;
        LOGGER.debug("Could not close {}, transactions still aren't finished yet. Retrying again in {}ms (Retry {} of {})",
                this.name, this.retryInterval, this.tries, this.maxTries);
        try {
            Thread.sleep(this.retryInterval);
            return close();
        } catch (InterruptedException e) {
            LOGGER.debug("Interrupted Exception when trying to close {}", this.name, e);
            Thread.currentThread().interrupt();
            return false;
        }
    }

    @VisibleForTesting
    protected int getTries() {
        return tries;
    }
}
