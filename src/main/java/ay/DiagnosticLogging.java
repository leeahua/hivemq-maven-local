package ay;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.FileAppender;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;

import java.util.Iterator;

public class DiagnosticLogging {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(DiagnosticLogging.class);

    protected static void setTraceLog(String path) {
        LOGGER.info("Creating trace log {}", path);
        ch.qos.logback.classic.Logger rootLogger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger("ROOT");
        Level level = rootLogger.getLevel();
        Iterator<Appender<ILoggingEvent>> appenderIterator = rootLogger.iteratorForAppenders();
        while (appenderIterator.hasNext()) {
            appenderIterator.next().addFilter(new PreserveOriginalLoggingLevelFilter(level));
        }
        ILoggerFactory loggerFactory = LoggerFactory.getILoggerFactory();
        PatternLayoutEncoder layoutEncoder = new PatternLayoutEncoder();
        layoutEncoder.setPattern("%date %level [%thread] %logger{10} [%file:%line] %msg%n");
        layoutEncoder.setContext((Context) loggerFactory);
        layoutEncoder.start();
        FileAppender fileAppender = new FileAppender();
        fileAppender.setFile(path);
        fileAppender.setEncoder(layoutEncoder);
        fileAppender.setContext((Context) loggerFactory);
        fileAppender.start();
        rootLogger.addAppender(fileAppender);
        rootLogger.setLevel(Level.ALL);
        rootLogger.setAdditive(false);
    }

    private static class PreserveOriginalLoggingLevelFilter extends Filter<ILoggingEvent> {
        private final Level level;

        PreserveOriginalLoggingLevelFilter(Level level) {
            this.level = level;
        }

        @Override
        public FilterReply decide(ILoggingEvent event) {
            if (event.getLevel().toInt() < this.level.toInt()) {
                return FilterReply.DENY;
            }
            return FilterReply.ACCEPT;
        }
    }
}
