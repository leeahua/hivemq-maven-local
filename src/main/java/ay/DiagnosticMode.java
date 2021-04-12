package ay;

import az.DiagnosticData;
import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.hivemq.spi.config.SystemInformation;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.util.Optional;


public class DiagnosticMode {
    private static final Logger LOGGER = LoggerFactory.getLogger(DiagnosticMode.class);
    private final DiagnosticData diagnosticData;
    private final SystemInformation systemInformation;

    @Inject
    DiagnosticMode(DiagnosticData diagnosticData,
                   SystemInformation systemInformation) {
        this.diagnosticData = diagnosticData;
        this.systemInformation = systemInformation;
    }

    @PostConstruct
    public void init() {
        Optional<File> mayDiagnosticsFolder = getDiagnosticsFolder();
        if (mayDiagnosticsFolder.isPresent()) {
            writeDiagnosticData(mayDiagnosticsFolder.get());
            DiagnosticLogging.setTraceLog(new File(mayDiagnosticsFolder.get(), "tracelog.log").getAbsolutePath());
            copyMigrationLog(mayDiagnosticsFolder);
        }
    }

    private void copyMigrationLog(Optional<File> mayDiagnosticsFolder) {
        File migrationLogFile = new File(this.systemInformation.getLogFolder(), "migration.log");
        if (migrationLogFile.exists()) {
            try {
                FileUtils.copyFileToDirectory(migrationLogFile, mayDiagnosticsFolder.get());
            } catch (IOException e) {
                LOGGER.error("Not able to copy migration log to diagnostics folder", e);
            }
        }
    }

    private void writeDiagnosticData(File diagnosticsFolder) {
        File diagnosticsFile = new File(diagnosticsFolder, "diagnostics.txt");
        try {
            LOGGER.info("Creating Diagnostics file: {}", diagnosticsFile.getAbsolutePath());
            diagnosticsFile.createNewFile();
            Files.write(this.diagnosticData.getContent(), diagnosticsFile, Charsets.UTF_8);
        } catch (IOException e) {
            LOGGER.error("Could not create the diagnostics.txt file. Stopping Diagnostic Mode");
        }
    }


    private Optional<File> getDiagnosticsFolder() {
        File homeFolder = this.systemInformation.getHiveMQHomeFolder();
        File diagnosticsFolder = new File(homeFolder, "diagnostics");
        if (diagnosticsFolder.exists()) {
            try {
                LOGGER.warn("Diagnostics folder already exists, deleting old folder");
                FileUtils.forceDelete(diagnosticsFolder);
            } catch (IOException e) {
                LOGGER.error("Could not delete diagnostics folder. Stopping Diagnostic Mode");
                return Optional.empty();
            }
        }
        try {
            LOGGER.info("Creating 'diagnostics' folder in HiveMQ home folder: {}", homeFolder.getAbsolutePath());
            FileUtils.forceMkdir(diagnosticsFolder);
        } catch (IOException e) {
            LOGGER.error("Could not create diagnostics folder. Stopping Diagnostic Mode");
            return Optional.empty();
        }
        return Optional.of(diagnosticsFolder);
    }
}
