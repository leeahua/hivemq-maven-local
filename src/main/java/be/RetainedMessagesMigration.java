package be;

import at1.Migration;
import at1.Migrations;
import av.PersistenceConfigurationService;
import av.PersistenceConfigurationService.PersistenceMode;
import av1.MetaInformation;
import av1.MetaInformationFile;
import bd1.PersistenceFolders;
import bf.RetainedMessageLocalXodusV030003Persistence;
import bg1.RetainedMessagesLocalXodusPersistence;
import com.hivemq.spi.config.SystemInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Provider;
import java.io.File;
import java.util.concurrent.atomic.AtomicLong;

public class RetainedMessagesMigration implements Migration {
    private static final Logger LOGGER = LoggerFactory.getLogger(RetainedMessagesMigration.class);
    private static final Logger MIGRATIONS_LOGGER = LoggerFactory.getLogger(Migrations.LOGGER_NAME);
    private final PersistenceFolders persistenceFolders;
    private final Provider<RetainedMessageLocalXodusV030003Persistence> v030003PersistenceProvider;
    private final Provider<RetainedMessagesLocalXodusPersistence> currentPersistenceProvider;
    private final SystemInformation systemInformation;
    private final PersistenceConfigurationService persistenceConfigurationService;

    @Inject
    public RetainedMessagesMigration(PersistenceFolders persistenceFolders,
                                     Provider<RetainedMessageLocalXodusV030003Persistence> v030003PersistenceProvider,
                                     Provider<RetainedMessagesLocalXodusPersistence> currentPersistenceProvider,
                                     SystemInformation systemInformation,
                                     PersistenceConfigurationService persistenceConfigurationService) {
        this.persistenceFolders = persistenceFolders;
        this.v030003PersistenceProvider = v030003PersistenceProvider;
        this.currentPersistenceProvider = currentPersistenceProvider;
        this.systemInformation = systemInformation;
        this.persistenceConfigurationService = persistenceConfigurationService;
    }

    public void migrate(String toVersion) {
        switch (toVersion) {
            case "3.1.0":
                migrateToV030100();
                break;
            default:
                throw new IllegalArgumentException("Unknown version " + toVersion + " for retained messages migration");
        }
    }

    private void migrateToV030100() {
        if (!needMigrateToV030100()) {
            return;
        }
        File persistenceFolder = this.persistenceFolders.root();
        File oldPersistenceFolder = new File(persistenceFolder, "retained_messages");
        if (!oldPersistenceFolder.exists()) {
            MIGRATIONS_LOGGER.info("no (old) persistence folder (retained_messages) present, skipping migration");
            LOGGER.debug("no (old) persistence folder present, skipping migration");
            return;
        }
        RetainedMessageLocalXodusV030003Persistence v030003Persistence = this.v030003PersistenceProvider.get();
        RetainedMessagesLocalXodusPersistence currentPersistence = this.currentPersistenceProvider.get();
        AtomicLong migratedCount = new AtomicLong(0L);
        v030003Persistence.migrate((topic, retainedMessage, timestamp) -> {
            currentPersistence.addOrReplace(topic, retainedMessage, timestamp != null ? timestamp : System.currentTimeMillis());
            migratedCount.getAndIncrement();
        });
        MIGRATIONS_LOGGER.info("Sucessfully migrated {} retained messages", migratedCount.get());
        LOGGER.debug("Sucessfully migrated {} retained messages", migratedCount.get());
        updateMetaInformationToV030100();
    }

    private void updateMetaInformationToV030100() {
        MetaInformation metaInformation = MetaInformationFile.read(this.systemInformation);
        metaInformation.setRetainedMessagesPersistenceVersion(RetainedMessagesLocalXodusPersistence.CURRENT_VERSION);
        metaInformation.setRetainedMessagesPersistenceMode(PersistenceMode.FILE);
        MetaInformationFile.write(this.systemInformation, metaInformation);
    }

    private boolean needMigrateToV030100() {
        MetaInformation metaInformation = MetaInformationFile.read(this.systemInformation);
        if (this.persistenceConfigurationService.getRetainedMessagesMode() == PersistenceMode.IN_MEMORY) {
            MIGRATIONS_LOGGER.info("Retained messages persistence is in-memory, skipping migration");
            LOGGER.debug("Retained messages persistence is in-memory, skipping migration");
            return false;
        }
        PersistenceMode persistenceMode = metaInformation.getRetainedMessagesPersistenceMode();
        if (persistenceMode != null && persistenceMode == PersistenceMode.IN_MEMORY) {
            MIGRATIONS_LOGGER.info("Retained messages persistence was in-memory, skipping migration");
            LOGGER.debug("Retained messages persistence was in-memory, skipping migration");
            return false;
        }
        String previousVersion = metaInformation.getRetainedMessagesPersistenceVersion();
        String currentVersion = RetainedMessagesLocalXodusPersistence.CURRENT_VERSION;
        if (previousVersion != null && previousVersion.equals(currentVersion)) {
            MIGRATIONS_LOGGER.info("Retained messages persistence is already migrated to current version {}, skipping migration", currentVersion);
            LOGGER.debug("Retained messages persistence is already migrated to current version, skipping migration");
            return false;
        }
        return true;
    }
}
