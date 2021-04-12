package ay;

import com.google.inject.AbstractModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DiagnosticModule extends AbstractModule {
    private static final Logger LOGGER = LoggerFactory.getLogger(DiagnosticModule.class);

    protected void configure() {
        boolean diagnosticMode = Boolean.parseBoolean(System.getProperty("diagnosticMode"));
        if (!diagnosticMode) {
            LOGGER.debug("Diagnostic mode is disabled");
            return;
        }
        LOGGER.info("Starting with Diagnostic mode");
        bind(DiagnosticMode.class).asEagerSingleton();
    }
}
