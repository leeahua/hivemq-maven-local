package at1;

import av.HiveMQConfigurationService;
import av1.MetaInformation;
import av1.MetaInformationFile;
import aw1.FilePersistenceMigration;
import com.google.inject.Injector;
import com.hivemq.spi.config.SystemInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

public class Migrations {
    public static final String START_VERSION = "3.0.3";
    public static final String LOGGER_NAME = "migrations";
    private static final Logger LOGGER = LoggerFactory.getLogger(Migrations.class);
    private static final Logger MIGRATIONS_LOGGER = LoggerFactory.getLogger(LOGGER_NAME);

    public static Map<MigrationType, Set<String>> getNeededMigrations(SystemInformation systemInformation) {
        MIGRATIONS_LOGGER.info("Checking for migrations (HiveMQ version {})", systemInformation.getHiveMQVersion());
        if (systemInformation.getHiveMQVersion().equals("Development Snapshot")) {
            MIGRATIONS_LOGGER.info("Skipping migration because it is a Development Snapshot");
            return Collections.emptyMap();
        }
        MetaInformation metaInformation = MetaInformationFile.read(systemInformation);
        if (!metaInformation.isDataFolderExists()) {
            LOGGER.trace("No data folder present, skip migrations.");
            MIGRATIONS_LOGGER.info("Skipping migration because no data folder is present");
            return Collections.emptyMap();
        }
        if (!metaInformation.isMetadataFileExists() &&
                !metaInformation.isPersistenceFolderExists()) {
            LOGGER.trace("Assuming a fresh HiveMQ installation. Skip migrations.");
            MIGRATIONS_LOGGER.info("Skipping migration because no meta file and no persistence folder is present, assuming new HiveMQ installation");
            return Collections.emptyMap();
        }
        String previousVersion;
        if (!metaInformation.isMetadataFileExists() &&
                metaInformation.isPersistenceFolderExists()) {
            LOGGER.trace("No meta file present, assuming HiveMQ version 3.0.x, migration needed.");
            MIGRATIONS_LOGGER.info("No meta file present, assuming HiveMQ version 3.0.3, migration needed.");
            previousVersion= START_VERSION;
            metaInformation = new MetaInformation();
            metaInformation.setHivemqVersion(START_VERSION);
            MetaInformationFile.write(systemInformation, metaInformation);
        } else {
            previousVersion = getReleaseVersion(metaInformation.getHivemqVersion());
        }
        String currentVersion = getReleaseVersion(systemInformation.getHiveMQVersion());
        if (previousVersion.equals(currentVersion)) {
            LOGGER.trace("Same HiveMQ version, skip migration.");
            MIGRATIONS_LOGGER.info("Same HiveMQ version, skip migration.");
            return Collections.emptyMap();
        }
        Map<MigrationType, Set<String>> neededMigrations = MigrationVersions.getNeededMigrations(previousVersion, currentVersion);
        MIGRATIONS_LOGGER.info("Found following needed migrations: {}", neededMigrations);
        return neededMigrations;
    }

    private static String getReleaseVersion(String version) {
        return version.replace("-SNAPSHOT", "");
    }

    public static void start(Injector injector,
                             Map<MigrationType, Set<String>> neededMigrations) {
        MIGRATIONS_LOGGER.info("Start migration.");
        FilePersistenceMigration migration = injector.getInstance(FilePersistenceMigration.class);
        migration.start(neededMigrations);
    }

    public static void finish(SystemInformation systemInformation,
                              HiveMQConfigurationService hiveMQConfigurationService) {
        MigrationFinisher finisher = new MigrationFinisher(systemInformation, hiveMQConfigurationService);
        finisher.finish();
        MIGRATIONS_LOGGER.info("Finished migration");
    }
}
