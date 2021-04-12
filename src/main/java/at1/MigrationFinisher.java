package at1;

import av.HiveMQConfigurationService;
import av1.MetaInformation;
import av1.MetaInformationFile;
import bg1.IncomingMessageFlowLocalXodusPersistence;
import bg1.OutgoingMessageFlowLocalXodusPersistence;
import bg1.RetainedMessagesLocalXodusPersistence;
import bi1.ClientSessionLocalXodusPersistence;
import bi1.ClientSessionSubscriptionsLocalXodusPersistence;
import bj1.QueuedMessagesLocalXodusPersistence;
import com.hivemq.spi.config.SystemInformation;

public class MigrationFinisher {
    private final SystemInformation systemInformation;
    private final HiveMQConfigurationService hiveMQConfigurationService;

    public MigrationFinisher(SystemInformation systemInformation,
                             HiveMQConfigurationService hiveMQConfigurationService) {
        this.systemInformation = systemInformation;
        this.hiveMQConfigurationService = hiveMQConfigurationService;
    }

    public void finish() {
        updateMetaInformationToCurrentVersion();
    }

    private void updateMetaInformationToCurrentVersion() {
        MetaInformation metaInformation = MetaInformationFile.read(this.systemInformation);
        metaInformation.setHivemqVersion(this.systemInformation.getHiveMQVersion());
        metaInformation.setClientSessionPersistenceVersion(ClientSessionLocalXodusPersistence.CURRENT_VERSION);
        metaInformation.setClientSessionPersistenceMode(this.hiveMQConfigurationService.persistenceConfiguration().getClientSessionGeneralMode());
        metaInformation.setIncomingMessageFlowPersistenceVersion(IncomingMessageFlowLocalXodusPersistence.CURRENT_VERSION);
        metaInformation.setIncomingMessageFlowPersistenceMode(this.hiveMQConfigurationService.persistenceConfiguration().getMessageFlowIncomingMode());
        metaInformation.setOutgoingMessageFlowPersistenceVersion(OutgoingMessageFlowLocalXodusPersistence.CURRENT_VERSION);
        metaInformation.setOutgoingMessageFlowPersistenceMode(this.hiveMQConfigurationService.persistenceConfiguration().getMessageFlowOutgoingMode());
        metaInformation.setQueuedMessagesPersistenceVersion(QueuedMessagesLocalXodusPersistence.CURRENT_VERSION);
        metaInformation.setQueuedMessagesPersistenceMode(this.hiveMQConfigurationService.persistenceConfiguration().getClientSessionQueuedMessagesMode());
        metaInformation.setRetainedMessagesPersistenceVersion(RetainedMessagesLocalXodusPersistence.CURRENT_VERSION);
        metaInformation.setRetainedMessagesPersistenceMode(this.hiveMQConfigurationService.persistenceConfiguration().getRetainedMessagesMode());
        metaInformation.setSubscriptionPersistenceVersion(ClientSessionSubscriptionsLocalXodusPersistence.CURRENT_VERSION);
        metaInformation.setSubscriptionPersistenceMode(this.hiveMQConfigurationService.persistenceConfiguration().getClientSessionSubscriptionMode());
        MetaInformationFile.write(this.systemInformation, metaInformation);
    }
}
