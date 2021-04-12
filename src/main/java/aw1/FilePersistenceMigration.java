package aw1;

import at1.Migration;
import at1.MigrationType;
import at1.Migrations;
import ax1.ClientSessionMigration;
import az1.IncomingMessageFlowMigration;
import ba.OutgoingMessageFlowMigration;
import bc.QueuedMessagesMigration;
import be.RetainedMessagesMigration;
import bg.ClientSubscriptionsMigration;
import d.CacheScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.Map;
import java.util.Set;

@CacheScoped
public class FilePersistenceMigration {
    private static final Logger LOGGER = LoggerFactory.getLogger(FilePersistenceMigration.class);
    private static final Logger MIGRATIONS_LOGGER = LoggerFactory.getLogger(Migrations.LOGGER_NAME);
    private final Provider<RetainedMessagesMigration> retainedMessagesMigrationProvider;
    private final Provider<OutgoingMessageFlowMigration> outgoingMessageFlowMigrationProvider;
    private final Provider<ClientSessionMigration> clientSessionMigrationProvider;
    private final Provider<IncomingMessageFlowMigration> incomingMessageFlowMigrationProvider;
    private final Provider<QueuedMessagesMigration> queuedMessagesMigrationProvider;
    private final Provider<ClientSubscriptionsMigration> clientSubscriptionsMigrationProvider;

    @Inject
    public FilePersistenceMigration(
            Provider<RetainedMessagesMigration> retainedMessagesMigrationProvider,
            Provider<OutgoingMessageFlowMigration> outgoingMessageFlowMigrationProvider,
            Provider<ClientSessionMigration> clientSessionMigrationProvider,
            Provider<IncomingMessageFlowMigration> incomingMessageFlowMigrationProvider,
            Provider<QueuedMessagesMigration> queuedMessagesMigrationProvider,
            Provider<ClientSubscriptionsMigration> clientSubscriptionsMigrationProvider) {
        this.retainedMessagesMigrationProvider = retainedMessagesMigrationProvider;
        this.outgoingMessageFlowMigrationProvider = outgoingMessageFlowMigrationProvider;
        this.clientSessionMigrationProvider = clientSessionMigrationProvider;
        this.incomingMessageFlowMigrationProvider = incomingMessageFlowMigrationProvider;
        this.queuedMessagesMigrationProvider = queuedMessagesMigrationProvider;
        this.clientSubscriptionsMigrationProvider = clientSubscriptionsMigrationProvider;
    }

    public void start(Map<MigrationType, Set<String>> neededMigrations) {
        long allStartMillis = System.currentTimeMillis();
        MIGRATIONS_LOGGER.info("Start File Persistence migration");
        LOGGER.info("Migrating File Persistences (this can take a few minutes) ...");
        neededMigrations.forEach((type, versions) -> {
            Migration migration = getMigration(type);
            versions.forEach(version -> {
                long startMillis = System.currentTimeMillis();
                MIGRATIONS_LOGGER.info("Migrating {} to version {} ...", type, version);
                LOGGER.debug("Migrating {} to version {} ...", type, version);
                migration.migrate(version);
                MIGRATIONS_LOGGER.info("Migrated {} to version {} successfully in {} ms",
                        type, version, System.currentTimeMillis() - startMillis);
                LOGGER.debug("Migrated {} to version {} successfully in {} ms",
                        type, version, System.currentTimeMillis() - startMillis);
            });
        });
        LOGGER.info("File Persistences successfully migrated in " + (System.currentTimeMillis() - allStartMillis) + " ms");
        MIGRATIONS_LOGGER.info("File Persistences successfully migrated in " + (System.currentTimeMillis() - allStartMillis) + " ms");
    }

    private Migration getMigration(MigrationType type) {
        if (type == MigrationType.FILE_PERSISTENCE_CLIENT_SESSIONS) {
            return clientSessionMigrationProvider.get();
        }
        if (type == MigrationType.FILE_PERSISTENCE_CLIENT_SESSION_QUEUED_MESSAGES) {
            return queuedMessagesMigrationProvider.get();
        }
        if (type == MigrationType.FILE_PERSISTENCE_CLIENT_SESSION_SUBSCRIPTIONS) {
            return clientSubscriptionsMigrationProvider.get();
        }
        if (type == MigrationType.FILE_PERSISTENCE_INCOMING_MESSAGE_FLOW) {
            return incomingMessageFlowMigrationProvider.get();
        }
        if (type == MigrationType.FILE_PERSISTENCE_OUTGOING_MESSAGE_FLOW) {
            return outgoingMessageFlowMigrationProvider.get();
        }
        if (type == MigrationType.FILE_PERSISTENCE_RETAINED_MESSAGES) {
            return retainedMessagesMigrationProvider.get();
        }
        return null;
    }
}
