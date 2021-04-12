package b;

import al1.ExodusLogModificator;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.logback.InstrumentedAppender;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import java.io.File;

public class LogConfigurator {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(LogConfigurator.class);

    public static void init(File configFolder, MetricRegistry metricRegistry) {
        loadConfig(configFolder);
        installBridge();
        setLoggerAppender(metricRegistry);
    }

    private static void setLoggerAppender(MetricRegistry metricRegistry) {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        ch.qos.logback.classic.Logger rootLogger = loggerContext.getLogger("ROOT");
        InstrumentedAppender appender = new InstrumentedAppender(metricRegistry);
        appender.setName("com.hivemq.logging");
        appender.setContext(rootLogger.getLoggerContext());
        appender.start();
        rootLogger.addAppender(appender);
    }

    private static void installBridge() {
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();
    }

    private static void loadConfig(File configFolder) {
        File logbackFile = new File(configFolder, "logback.xml");
        if (!logbackFile.canRead()) {
            LOGGER.debug("Using default log configuration");
            return;
        }
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        try {
            loggerContext.reset();
            JoranConfigurator configurator = new JoranConfigurator();
            configurator.setContext(loggerContext);
            configurator.doConfigure(logbackFile);
        } catch (JoranException e) {
        } catch (Exception e) {
            e.printStackTrace();
        }
        StatusPrinter.printInCaseOfErrorsOrWarnings(loggerContext);
        LOGGER.info("Log Configuration was overridden by {}", logbackFile.getAbsolutePath());
    }

    public static void addXodusLogModificator() {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        context.addTurboFilter(new ExodusLogModificator());
        LOGGER.trace("Added Xodus Log Level Modificator");
    }
}
