package co;

import ch.qos.logback.classic.Level;
import com.hivemq.spi.annotations.NotNull;
import com.hivemq.spi.services.LogService;
import d.CacheScoped;
import org.slf4j.LoggerFactory;

@CacheScoped
public class LogServiceImpl implements LogService {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(LogServiceImpl.class);

    public void setLogLevel(@NotNull LogLevel level) {
        if (level == null) {
            return;
        }
        LOGGER.info("Setting log level to {}", level);
        ch.qos.logback.classic.Logger rootLogger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger("ROOT");
        switch (level) {
            case TRACE:
                rootLogger.setLevel(Level.TRACE);
                break;
            case DEBUG:
                rootLogger.setLevel(Level.DEBUG);
                break;
            case INFO:
                rootLogger.setLevel(Level.INFO);
                break;
            case WARN:
                rootLogger.setLevel(Level.WARN);
                break;
            case ERROR:
                rootLogger.setLevel(Level.ERROR);
        }
    }

    public LogLevel getLogLevel() {
        ch.qos.logback.classic.Logger rootLogger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger("ROOT");
        Level level = rootLogger.getLevel();
        switch (level.toInt()) {
            case Level.DEBUG_INT:
                return LogLevel.DEBUG;
            case Level.INFO_INT:
                return LogLevel.INFO;
            case Level.WARN_INT:
                return LogLevel.WARN;
            case Level.ERROR_INT:
                return LogLevel.ERROR;
        }
        return LogLevel.TRACE;
    }
}
