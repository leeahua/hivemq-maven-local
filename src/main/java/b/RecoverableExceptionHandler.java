package b;

import com.google.common.base.Throwables;
import com.google.inject.CreationException;
import com.google.inject.ProvisionException;
import com.hivemq.spi.exceptions.UnrecoverableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RecoverableExceptionHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(RecoverableExceptionHandler.class);

    public static void init() {
        Thread.setDefaultUncaughtExceptionHandler((thread, cause) -> {
            if (cause instanceof UnrecoverableException) {
                if (((UnrecoverableException) cause).isShowException()) {
                    LOGGER.error("An unrecoverable Exception occurred. Exiting HiveMQ",
                            thread, cause);
                }
                System.exit(1);
            } else if (cause instanceof CreationException) {
                if (cause.getCause() instanceof UnrecoverableException) {
                    LOGGER.error("An unrecoverable Exception occurred. Exiting HiveMQ");
                    System.exit(1);
                }
            } else if (cause instanceof ProvisionException &&
                    cause.getCause() instanceof UnrecoverableException) {
                LOGGER.error("An unrecoverable Exception occurred. Exiting HiveMQ");
                System.exit(1);
            }
            Throwable rootCase = Throwables.getRootCause(cause);
            if (rootCase instanceof UnrecoverableException) {
                if (((UnrecoverableException) rootCase).isShowException()) {
                    LOGGER.error("Cause: ", cause);
                }
            } else {
                LOGGER.error("Uncaught Error:", cause);
            }
        });
    }
}
