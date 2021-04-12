package ax1;

import at1.Migration;
import at1.Migrations;
import av.PersistenceConfigurationService;
import av.PersistenceConfigurationService.PersistenceMode;
import av1.MetaInformation;
import av1.MetaInformationFile;
import ay1.ClientSessionLocalXodusV030003Persistence;
import bd1.PersistenceFolders;
import bi1.ClientSessionLocalXodusPersistence;
import com.hivemq.spi.config.SystemInformation;
import i.ClusterIdProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import v.ClientSession;

import javax.inject.Inject;
import javax.inject.Provider;
import java.io.File;
import java.util.concurrent.atomic.AtomicLong;

public class ClientSessionMigration implements Migration {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientSessionMigration.class);
    private static final Logger MIGRATIONS_LOGGER = LoggerFactory.getLogger(Migrations.LOGGER_NAME);
    private final ClusterIdProducer clusterIdProducer;
    private final PersistenceFolders persistenceFolders;
    private final SystemInformation systemInformation;
    private final PersistenceConfigurationService persistenceConfigurationService;
    private final Provider<ClientSessionLocalXodusV030003Persistence> v030003PersistenceProvider;
    private final Provider<ClientSessionLocalXodusPersistence> currentPersistenceProvider;

    @Inject
    public ClientSessionMigration(ClusterIdProducer clusterIdProducer,
                                  PersistenceFolders persistenceFolders,
                                  SystemInformation systemInformation,
                                  PersistenceConfigurationService persistenceConfigurationService,
                                  Provider<ClientSessionLocalXodusV030003Persistence> v030003PersistenceProvider,
                                  Provider<ClientSessionLocalXodusPersistence> currentPersistenceProvider) {
        this.clusterIdProducer = clusterIdProducer;
        this.persistenceFolders = persistenceFolders;
        this.systemInformation = systemInformation;
        this.persistenceConfigurationService = persistenceConfigurationService;
        this.v030003PersistenceProvider = v030003PersistenceProvider;
        this.currentPersistenceProvider = currentPersistenceProvider;
    }

    public void migrate(String toVersion) {
        switch (toVersion) {
            case "3.1.0":
                migrateToV030100();
                break;
            default:
                throw new IllegalArgumentException("Unknown version " + toVersion + " for client session migration");
        }
    }

    private void migrateToV030100() {
        if (!needMigrateToV030100()) {
            return;
        }
        File persistenceFolder = this.persistenceFolders.root();
        File oldPersistenceFolder = new File(persistenceFolder, "client_session_store_0");
        if (!oldPersistenceFolder.exists()) {
            LOGGER.debug("no (old) persistence folder present, skipping migration");
            return;
        }
        ClientSessionLocalXodusV030003Persistence v030003Persistence = this.v030003PersistenceProvider.get();
        ClientSessionLocalXodusPersistence currentPerisistence = this.currentPersistenceProvider.get();
        long size = v030003Persistence.size();
        LOGGER.debug("Migrating {} client sessions ...", size);
        if (size == 0L) {
            return;
        }
        AtomicLong migratedCount = new AtomicLong(0L);
        v030003Persistence.migrate(((clientId, timestamp) -> {
            currentPerisistence.persistent(clientId, new ClientSession(false, true, clusterIdProducer.get()),
                    timestamp != null ? timestamp : System.currentTimeMillis());
            migratedCount.getAndIncrement();
        }));
        LOGGER.debug("Sucessfully migrated {} client sessions", migratedCount.get());
        updateMetaInformationToV030100();
        v030003Persistence.close();
    }

    private void updateMetaInformationToV030100() {
        MetaInformation metaInformation = MetaInformationFile.read(this.systemInformation);
        metaInformation.setClientSessionPersistenceVersion(ClientSessionLocalXodusPersistence.CURRENT_VERSION);
        metaInformation.setClientSessionPersistenceMode(PersistenceMode.FILE);
        MetaInformationFile.write(this.systemInformation, metaInformation);
    }

    private boolean needMigrateToV030100() {
        MetaInformation metaInformation = MetaInformationFile.read(this.systemInformation);
        if (this.persistenceConfigurationService.getClientSessionGeneralMode() ==
                PersistenceMode.IN_MEMORY) {
            MIGRATIONS_LOGGER.info("Client session persistence is in-memory, skipping migration");
            LOGGER.debug("Client session persistence is in-memory, skipping migration");
            return false;
        }
        PersistenceMode mode = metaInformation.getClientSessionPersistenceMode();
        if (mode != null && mode == PersistenceMode.IN_MEMORY) {
            MIGRATIONS_LOGGER.info("Client session persistence was in-memory, skipping migration");
            LOGGER.debug("Client session persistence was in-memory, skipping migration");
            return false;
        }
        String previousVersion = metaInformation.getClientSessionPersistenceVersion();
        String currentVersion = ClientSessionLocalXodusPersistence.CURRENT_VERSION;
        if (previousVersion != null && previousVersion.equals(currentVersion)) {
            MIGRATIONS_LOGGER.info("Client session persistence is already migrated to current version {}, skipping migration",
                    currentVersion);
            LOGGER.debug("Client session persistence is already migrated to current version, skipping migration");
            return false;
        }
        return true;
    }
}
