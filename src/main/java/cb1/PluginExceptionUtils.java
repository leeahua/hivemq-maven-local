package cb1;

import com.google.common.base.Throwables;
import com.hivemq.spi.annotations.NotNull;
import com.hivemq.spi.exceptions.UnrecoverableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PluginExceptionUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(PluginExceptionUtils.class);

    public static void logAndThrow(@NotNull Throwable cause) {
        handle(cause, () -> {
            LOGGER.error("An unhandled exception was thrown by a plugin. This is fatal because this kind of exceptions must be handled by the plugin.");
            LOGGER.error("Original Exception:", cause);
            throw new UnrecoverableException(false);
        });
    }

    public static void log(@NotNull Throwable cause) {
        handle(cause, () -> {
            LOGGER.error("An unhandled exception was thrown by a plugin. Plugins are responsible on their own to handle exceptions.");
            LOGGER.error("Original Exception:", cause);
        });
    }

    public static void logOrThrow(String message, Throwable cause) {
        if (cause instanceof Error) {
            throw ((Error) cause);
        }
        LOGGER.error(message + " " + cause);
    }

    private static void handle(Throwable cause, Handler handler) {
        if (cause == null) {
            LOGGER.debug("Null was passed to guardFromPluginThrowableAndStop. This is fatal and must not happen!");
        }
        if (cause instanceof Error) {
            Throwables.propagateIfPossible(cause);
        } else {
            handler.handle();
        }
    }

    interface Handler {
        void handle();
    }
}
