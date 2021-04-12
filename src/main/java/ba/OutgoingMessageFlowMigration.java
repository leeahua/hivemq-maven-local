package ba;

import at1.Migration;
import at1.Migrations;
import av.PersistenceConfigurationService;
import av.PersistenceConfigurationService.PersistenceMode;
import av1.MetaInformation;
import av1.MetaInformationFile;
import bb.OutgoingMessageFlowLocalXodusV030003Persistence;
import bd1.PersistenceFolders;
import bg1.OutgoingMessageFlowLocalXodusPersistence;
import bu.InternalPublish;
import com.hivemq.spi.config.SystemInformation;
import com.hivemq.spi.message.Publish;
import i.ClusterIdProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Provider;
import java.io.File;
import java.util.concurrent.atomic.AtomicLong;

public class OutgoingMessageFlowMigration implements Migration {
    private static final Logger LOGGER = LoggerFactory.getLogger(OutgoingMessageFlowMigration.class);
    private static final Logger MIGRATIONS_LOGGER = LoggerFactory.getLogger(Migrations.LOGGER_NAME);
    private final PersistenceFolders persistenceFolders;
    private final Provider<OutgoingMessageFlowLocalXodusV030003Persistence> v030003PersistenceProvider;
    private final Provider<OutgoingMessageFlowLocalXodusPersistence> currentPersistenceProvider;
    private final ClusterIdProducer clusterIdProducer;
    private final SystemInformation systemInformation;
    private final PersistenceConfigurationService persistenceConfigurationService;

    @Inject
    public OutgoingMessageFlowMigration(PersistenceFolders persistenceFolders,
                                        Provider<OutgoingMessageFlowLocalXodusV030003Persistence> v030003PersistenceProvider,
                                        Provider<OutgoingMessageFlowLocalXodusPersistence> currentPersistenceProvider,
                                        ClusterIdProducer clusterIdProducer,
                                        SystemInformation systemInformation,
                                        PersistenceConfigurationService persistenceConfigurationService) {
        this.persistenceFolders = persistenceFolders;
        this.v030003PersistenceProvider = v030003PersistenceProvider;
        this.currentPersistenceProvider = currentPersistenceProvider;
        this.clusterIdProducer = clusterIdProducer;
        this.systemInformation = systemInformation;
        this.persistenceConfigurationService = persistenceConfigurationService;
    }

    public void migrate(String toVersion) {
        switch (toVersion) {
            case "3.1.0":
                migrateToV030100();
                break;
            default:
                throw new IllegalArgumentException("Unknown version " + toVersion + " for outgoing message flow migration");
        }
    }

    private void migrateToV030100() {
        if (!needMigrateToV030100()) {
            return;
        }
        File persistenceFolder = this.persistenceFolders.root();
        File oldPersistenceFolder = new File(persistenceFolder, "outgoing_message_flow_0");
        if (!oldPersistenceFolder.exists()) {
            MIGRATIONS_LOGGER.info("no (old) persistence folder (outgoing_message_flow_0) present, skipping migration");
            LOGGER.debug("no (old) persistence folder present, skipping migration");
            return;
        }
        OutgoingMessageFlowLocalXodusV030003Persistence v030003Persistence = this.v030003PersistenceProvider.get();
        OutgoingMessageFlowLocalXodusPersistence currentPersistence = this.currentPersistenceProvider.get();
        LOGGER.debug("Migrating outgoing message flows.");
        AtomicLong migratedCount = new AtomicLong(0L);
        v030003Persistence.migrate((clientId, messageId, message) -> {
            if (message instanceof Publish) {
                if (!(message instanceof InternalPublish)) {
                    currentPersistence.addOrReplace(clientId, messageId, new InternalPublish(clusterIdProducer.get(), (Publish) message));
                } else {
                    currentPersistence.addOrReplace(clientId, messageId, message);
                }
            } else {
                currentPersistence.addOrReplace(clientId, messageId, message);
            }
            migratedCount.getAndIncrement();
        });
        MIGRATIONS_LOGGER.info("Sucessfully migrated {} outgoing message flows", migratedCount.get());
        LOGGER.debug("Sucessfully migrated {} outgoing message flows", migratedCount.get());
        v030003Persistence.close();
        updateMetaInformationToV030100();
    }

    private void updateMetaInformationToV030100() {
        MetaInformation metaInformation = MetaInformationFile.read(this.systemInformation);
        metaInformation.setOutgoingMessageFlowPersistenceVersion(OutgoingMessageFlowLocalXodusPersistence.CURRENT_VERSION);
        metaInformation.setOutgoingMessageFlowPersistenceMode(PersistenceMode.FILE);
        MetaInformationFile.write(this.systemInformation, metaInformation);
    }

    private boolean needMigrateToV030100() {
        MetaInformation metaInformation = MetaInformationFile.read(this.systemInformation);
        if (this.persistenceConfigurationService.getMessageFlowOutgoingMode() == PersistenceMode.IN_MEMORY) {
            MIGRATIONS_LOGGER.info("Outgoing message flow persistence is in-memory, skipping migration");
            LOGGER.debug("Outgoing message flow persistence is in-memory, skipping migration");
            return false;
        }
        PersistenceMode mode = metaInformation.getOutgoingMessageFlowPersistenceMode();
        if (mode != null && mode == PersistenceMode.IN_MEMORY) {
            MIGRATIONS_LOGGER.info("Outgoing message flow persistence was in-memory, skipping migration");
            LOGGER.debug("Outgoing message flow persistence was in-memory, skipping migration");
            return false;
        }
        String previousVersion = metaInformation.getOutgoingMessageFlowPersistenceVersion();
        String currentVersion = OutgoingMessageFlowLocalXodusPersistence.CURRENT_VERSION;
        if (previousVersion != null && previousVersion.equals(currentVersion)) {
            MIGRATIONS_LOGGER.info("Outgoing message flow persistence is already migrated to current version, skipping migration");
            LOGGER.debug("Outgoing message flow persistence is already migrated to current version, skipping migration");
            return false;
        }
        return true;
    }
}
