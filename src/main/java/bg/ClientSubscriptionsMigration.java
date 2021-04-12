package bg;

import at1.Migration;
import at1.Migrations;
import av.PersistenceConfigurationService;
import av.PersistenceConfigurationService.PersistenceMode;
import av1.MetaInformation;
import av1.MetaInformationFile;
import bd1.PersistenceFolders;
import bh.ClientSessionSubscriptionLocalXodusV030003Persistence;
import bi1.ClientSessionLocalXodusPersistence;
import bi1.ClientSessionSubscriptionsLocalXodusPersistence;
import com.hivemq.spi.config.SystemInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Provider;
import java.io.File;
import java.util.concurrent.atomic.AtomicLong;

public class ClientSubscriptionsMigration implements Migration {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientSubscriptionsMigration.class);
    private static final Logger MIGRATIONS_LOGGER = LoggerFactory.getLogger(Migrations.LOGGER_NAME);
    private final PersistenceFolders persistenceFolders;
    private final Provider<ClientSessionSubscriptionLocalXodusV030003Persistence> v030003PersistenceProvider;
    private final Provider<ClientSessionSubscriptionsLocalXodusPersistence> currentPersistenceProvider;
    private final SystemInformation systemInformation;
    private final PersistenceConfigurationService persistenceConfigurationService;

    @Inject
    public ClientSubscriptionsMigration(PersistenceFolders persistenceFolders,
                                        Provider<ClientSessionSubscriptionLocalXodusV030003Persistence> v030003PersistenceProvider,
                                        Provider<ClientSessionSubscriptionsLocalXodusPersistence> currentPersistenceProvider,
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
                throw new IllegalArgumentException("Unknown version " + toVersion + " for client subscriptions migration");
        }
    }

    private void migrateToV030100() {
        if (!needMigrateToV030100()) {
            return;
        }
        File persistenceFolder = this.persistenceFolders.root();
        File oldPersistenceFolder = new File(persistenceFolder, "client_session_subscriptions_0");
        if (!oldPersistenceFolder.exists()) {
            MIGRATIONS_LOGGER.info("No (old) persistence folder (client_session_subscriptions_0) present, skipping migration");
            LOGGER.trace("no (old) persistence folder present, skipping migration");
            return;
        }
        ClientSessionSubscriptionLocalXodusV030003Persistence v030003Persistence = this.v030003PersistenceProvider.get();
        ClientSessionSubscriptionsLocalXodusPersistence currentPersistence = this.currentPersistenceProvider.get();
        AtomicLong migratedCount = new AtomicLong(0L);
        v030003Persistence.migrate((clientId, topic) -> {
            currentPersistence.addSubscription(clientId, topic, System.currentTimeMillis());
            migratedCount.getAndIncrement();
        });
        MIGRATIONS_LOGGER.info("Sucessfully migrated {} subscriptions", migratedCount.get());
        LOGGER.debug("Sucessfully migrated {} subscriptions", migratedCount.get());
        v030003Persistence.close();
        updateMetaInformationToV030100();
    }

    private void updateMetaInformationToV030100() {
        MetaInformation metaInformation = MetaInformationFile.read(this.systemInformation);
        metaInformation.setSubscriptionPersistenceVersion(ClientSessionSubscriptionsLocalXodusPersistence.CURRENT_VERSION);
        metaInformation.setSubscriptionPersistenceMode(PersistenceMode.FILE);
        MetaInformationFile.write(this.systemInformation, metaInformation);
    }

    private boolean needMigrateToV030100() {
        MetaInformation metaInformation = MetaInformationFile.read(this.systemInformation);
        if (this.persistenceConfigurationService.getClientSessionSubscriptionMode() == PersistenceMode.IN_MEMORY) {
            MIGRATIONS_LOGGER.info("Subscription persistence is in-memory, skipping migration");
            LOGGER.debug("Subscription persistence is in-memory, skipping migration");
            return false;
        }
        PersistenceMode mode = metaInformation.getSubscriptionPersistenceMode();
        if (mode != null && mode == PersistenceMode.IN_MEMORY) {
            MIGRATIONS_LOGGER.info("Subscription persistence was in-memory, skipping migration");
            LOGGER.debug("Subscription persistence was in-memory, skipping migration");
            return false;
        }
        String previousVersion = metaInformation.getSubscriptionPersistenceVersion();
        String currentVersion = ClientSessionLocalXodusPersistence.CURRENT_VERSION;
        if (previousVersion != null && previousVersion.equals(currentVersion)) {
            MIGRATIONS_LOGGER.info("Subscription persistence is already migrated to current version {}, skipping migration",
                    currentVersion);
            LOGGER.debug("Subscription persistence is already migrated to current version, skipping migration");
            return false;
        }
        return true;
    }
}
