package bc;

import at1.Migration;
import at1.Migrations;
import av.PersistenceConfigurationService;
import av.PersistenceConfigurationService.PersistenceMode;
import av1.MetaInformation;
import av1.MetaInformationFile;
import bd.QueuedMessagesLocalXodusV030003Persistence;
import bd1.PersistenceFolders;
import bj1.QueuedMessagesLocalXodusPersistence;
import bu.InternalPublish;
import com.hivemq.spi.config.SystemInformation;
import i.ClusterIdProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Provider;
import java.io.File;
import java.util.concurrent.atomic.AtomicLong;

public class QueuedMessagesMigration implements Migration {
    private static final Logger LOGGER = LoggerFactory.getLogger(QueuedMessagesMigration.class);
    private static final Logger MIGRATIONS_LOGGER = LoggerFactory.getLogger(Migrations.LOGGER_NAME);
    private final PersistenceFolders persistenceFolders;
    private final ClusterIdProducer clusterIdProducer;
    private final Provider<QueuedMessagesLocalXodusV030003Persistence> v030003PersistenceProvider;
    private final Provider<QueuedMessagesLocalXodusPersistence> currentPersistenceProvider;
    private final PersistenceConfigurationService persistenceConfigurationService;
    private final SystemInformation systemInformation;

    @Inject
    public QueuedMessagesMigration(PersistenceFolders persistenceFolders,
                                   ClusterIdProducer clusterIdProducer,
                                   Provider<QueuedMessagesLocalXodusV030003Persistence> v030003PersistenceProvider,
                                   Provider<QueuedMessagesLocalXodusPersistence> currentPersistenceProvider,
                                   PersistenceConfigurationService persistenceConfigurationService,
                                   SystemInformation systemInformation) {
        this.clusterIdProducer = clusterIdProducer;
        this.persistenceFolders = persistenceFolders;
        this.v030003PersistenceProvider = v030003PersistenceProvider;
        this.currentPersistenceProvider = currentPersistenceProvider;
        this.persistenceConfigurationService = persistenceConfigurationService;
        this.systemInformation = systemInformation;
    }

    public void migrate(String toVersion) {
        switch (toVersion) {
            case "3.1.0":
                migrateToV030100();
                break;
            default:
                throw new IllegalArgumentException("Unknown version " + toVersion + " for queued messages migration");
        }
    }

    private void migrateToV030100() {
        if (!needMigrateToV030100()) {
            return;
        }
        File persistenceFolder = this.persistenceFolders.root();
        File oldPersistenceFolder = new File(persistenceFolder, "queued_messages");
        if (!oldPersistenceFolder.exists()) {
            MIGRATIONS_LOGGER.info("no (old) persistence folder (queued_messages) present, skipping migration");
            LOGGER.debug("no (old) persistence folder present, skipping migration");
            return;
        }
        QueuedMessagesLocalXodusV030003Persistence v030003Persistence = this.v030003PersistenceProvider.get();
        QueuedMessagesLocalXodusPersistence currentPersistence = this.currentPersistenceProvider.get();
        AtomicLong migratedCount = new AtomicLong(0L);
        v030003Persistence.migrate((clientId, entry) -> {
            InternalPublish publish = new InternalPublish(clusterIdProducer.get(), entry.getTimestamp());
            publish.setPayload(entry.getPayload());
            publish.setQoS(entry.getQoS());
            publish.setTopic(entry.getTopic());
            currentPersistence.offer(clientId, publish, entry.getTimestamp());
            migratedCount.getAndIncrement();
        });
        MIGRATIONS_LOGGER.info("Sucessfully migrated {} queued messages", migratedCount.get());
        LOGGER.debug("Sucessfully migrated {} queued messages", migratedCount.get());
        v030003Persistence.close();
        updateMetaInformationToV030100();
    }

    private void updateMetaInformationToV030100() {
        MetaInformation metaInformation = MetaInformationFile.read(this.systemInformation);
        metaInformation.setQueuedMessagesPersistenceVersion(QueuedMessagesLocalXodusPersistence.CURRENT_VERSION);
        metaInformation.setQueuedMessagesPersistenceMode(PersistenceMode.FILE);
        MetaInformationFile.write(this.systemInformation, metaInformation);
    }

    private boolean needMigrateToV030100() {
        MetaInformation metaInformation = MetaInformationFile.read(this.systemInformation);
        if (this.persistenceConfigurationService.getClientSessionQueuedMessagesMode() == PersistenceMode.IN_MEMORY) {
            MIGRATIONS_LOGGER.info("Queued messages persistence is in-memory, skipping migration");
            LOGGER.debug("Queued messages persistence is in-memory, skipping migration");
            return false;
        }
        PersistenceMode mode = metaInformation.getQueuedMessagesPersistenceMode();
        if (mode != null && mode == PersistenceMode.IN_MEMORY) {
            MIGRATIONS_LOGGER.info("Queued messages persistence was in-memory, skipping migration");
            LOGGER.debug("Queued messages persistence was in-memory, skipping migration");
            return false;
        }
        String previousVersion = metaInformation.getQueuedMessagesPersistenceVersion();
        String currentVersion = QueuedMessagesLocalXodusPersistence.CURRENT_VERSION;
        if (previousVersion != null && previousVersion.equals(currentVersion)) {
            MIGRATIONS_LOGGER.info("Queued messages persistence is already migrated to current version {}, skipping migration", currentVersion);
            LOGGER.debug("Queued messages persistence is already migrated to current version, skipping migration");
            return false;
        }
        return true;
    }
}
