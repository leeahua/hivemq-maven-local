package al1;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.turbo.TurboFilter;
import ch.qos.logback.core.spi.FilterReply;
import jetbrains.exodus.ExodusException;
import jetbrains.exodus.env.EnvironmentImpl;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;

public class ExodusLogModificator extends TurboFilter {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(EnvironmentImpl.class);

    public FilterReply decide(Marker marker,
                              ch.qos.logback.classic.Logger logger,
                              Level level,
                              String format,
                              Object[] params,
                              Throwable t) {
        if (level.isGreaterOrEqual(Level.INFO) &&
                logger.getName().equals(LOGGER.getName()) &&
                format != null) {
            if (format.contains("transaction(s) not finished")) {
                logger.trace(marker, format, params);
                return FilterReply.DENY;
            }
            if (format.contains("Transactions stack traces are not available")) {
                logger.trace(marker, format, params);
                return FilterReply.DENY;
            }
        }
        if (level.isGreaterOrEqual(Level.ERROR) &&
                t != null &&
                t.getMessage() != null &&
                (t instanceof ExodusException) &&
                (t.getMessage().contains("cleanFile") ||
                        t.getMessage().contains("There is no file by address"))) {
            logger.trace(marker, "Xodus background job unable to cleanup stale data just now, trying again later");
            return FilterReply.DENY;
        }
        return FilterReply.NEUTRAL;
    }
}
