package av1;

import av.PersistenceConfigurationService.PersistenceMode;
import com.hivemq.spi.annotations.Nullable;

public class MetaInformation {
    private String hivemqVersion = null;
    private String clientSessionPersistenceVersion = null;
    private String queuedMessagesPersistenceVersion = null;
    private String subscriptionPersistenceVersion = null;
    private String incomingMessageFlowPersistenceVersion = null;
    private String outgoingMessageFlowPersistenceVersion = null;
    private String retainedMessagesPersistenceVersion = null;
    private PersistenceMode clientSessionPersistenceMode = null;
    private PersistenceMode queuedMessagesPersistenceMode = null;
    private PersistenceMode subscriptionPersistenceMode = null;
    private PersistenceMode incomingMessageFlowPersistenceMode = null;
    private PersistenceMode outgoingMessageFlowPersistenceMode = null;
    private PersistenceMode retainedMessagesPersistenceMode = null;
    private boolean dataFolderExists = false;
    private boolean persistenceFolderExists = false;
    private boolean metadataFileExists = false;

    @Nullable
    public String getHivemqVersion() {
        return hivemqVersion;
    }

    public void setHivemqVersion(String hivemqVersion) {
        this.hivemqVersion = hivemqVersion;
    }

    public String getClientSessionPersistenceVersion() {
        return clientSessionPersistenceVersion;
    }

    public void setClientSessionPersistenceVersion(String clientSessionPersistenceVersion) {
        this.clientSessionPersistenceVersion = clientSessionPersistenceVersion;
    }

    public String getQueuedMessagesPersistenceVersion() {
        return queuedMessagesPersistenceVersion;
    }

    public void setQueuedMessagesPersistenceVersion(String queuedMessagesPersistenceVersion) {
        this.queuedMessagesPersistenceVersion = queuedMessagesPersistenceVersion;
    }

    public String getSubscriptionPersistenceVersion() {
        return subscriptionPersistenceVersion;
    }

    public void setSubscriptionPersistenceVersion(String subscriptionPersistenceVersion) {
        this.subscriptionPersistenceVersion = subscriptionPersistenceVersion;
    }

    public String getIncomingMessageFlowPersistenceVersion() {
        return incomingMessageFlowPersistenceVersion;
    }

    public void setIncomingMessageFlowPersistenceVersion(String incomingMessageFlowPersistenceVersion) {
        this.incomingMessageFlowPersistenceVersion = incomingMessageFlowPersistenceVersion;
    }

    public String getOutgoingMessageFlowPersistenceVersion() {
        return outgoingMessageFlowPersistenceVersion;
    }

    public void setOutgoingMessageFlowPersistenceVersion(String outgoingMessageFlowPersistenceVersion) {
        this.outgoingMessageFlowPersistenceVersion = outgoingMessageFlowPersistenceVersion;
    }

    public String getRetainedMessagesPersistenceVersion() {
        return retainedMessagesPersistenceVersion;
    }

    public void setRetainedMessagesPersistenceVersion(String retainedMessagesPersistenceVersion) {
        this.retainedMessagesPersistenceVersion = retainedMessagesPersistenceVersion;
    }

    public PersistenceMode getClientSessionPersistenceMode() {
        return clientSessionPersistenceMode;
    }

    public void setClientSessionPersistenceMode(PersistenceMode clientSessionPersistenceMode) {
        this.clientSessionPersistenceMode = clientSessionPersistenceMode;
    }

    public PersistenceMode getQueuedMessagesPersistenceMode() {
        return queuedMessagesPersistenceMode;
    }

    public void setQueuedMessagesPersistenceMode(PersistenceMode queuedMessagesPersistenceMode) {
        this.queuedMessagesPersistenceMode = queuedMessagesPersistenceMode;
    }

    public PersistenceMode getSubscriptionPersistenceMode() {
        return subscriptionPersistenceMode;
    }

    public void setSubscriptionPersistenceMode(PersistenceMode subscriptionPersistenceMode) {
        this.subscriptionPersistenceMode = subscriptionPersistenceMode;
    }

    public PersistenceMode getIncomingMessageFlowPersistenceMode() {
        return incomingMessageFlowPersistenceMode;
    }

    public void setIncomingMessageFlowPersistenceMode(PersistenceMode incomingMessageFlowPersistenceMode) {
        this.incomingMessageFlowPersistenceMode = incomingMessageFlowPersistenceMode;
    }

    public PersistenceMode getOutgoingMessageFlowPersistenceMode() {
        return outgoingMessageFlowPersistenceMode;
    }

    public void setOutgoingMessageFlowPersistenceMode(PersistenceMode outgoingMessageFlowPersistenceMode) {
        this.outgoingMessageFlowPersistenceMode = outgoingMessageFlowPersistenceMode;
    }

    public PersistenceMode getRetainedMessagesPersistenceMode() {
        return retainedMessagesPersistenceMode;
    }

    public void setRetainedMessagesPersistenceMode(PersistenceMode retainedMessagesPersistenceMode) {
        this.retainedMessagesPersistenceMode = retainedMessagesPersistenceMode;
    }

    public boolean isDataFolderExists() {
        return dataFolderExists;
    }

    public void setDataFolderExists(boolean dataFolderExists) {
        this.dataFolderExists = dataFolderExists;
    }

    public boolean isPersistenceFolderExists() {
        return persistenceFolderExists;
    }

    public void setPersistenceFolderExists(boolean persistenceFolderExists) {
        this.persistenceFolderExists = persistenceFolderExists;
    }

    public boolean isMetadataFileExists() {
        return metadataFileExists;
    }

    public void setMetadataFileExists(boolean metadataFileExists) {
        this.metadataFileExists = metadataFileExists;
    }

    public String toString() {
        return "MetaInformation{hivemqVersion='" + this.hivemqVersion + '\'' +
                ", clientSessionPersistenceVersion='" + this.clientSessionPersistenceVersion + '\'' +
                ", queuedMessagesPersistenceVersion='" + this.queuedMessagesPersistenceVersion + '\'' +
                ", subscriptionPersistenceVersion='" + this.subscriptionPersistenceVersion + '\'' +
                ", incomingMessageFlowPersistenceVersion='" + this.incomingMessageFlowPersistenceVersion + '\'' +
                ", outgoingMessageFlowPersistenceVersion='" + this.outgoingMessageFlowPersistenceVersion + '\'' +
                ", retainedMessagesPersistenceVersion='" + this.retainedMessagesPersistenceVersion + '\'' +
                ", clientSessionPersistenceMode=" + this.clientSessionPersistenceMode +
                ", queuedMessagesPersistenceMode=" + this.queuedMessagesPersistenceMode +
                ", subscriptionPersistenceMode=" + this.subscriptionPersistenceMode +
                ", incomingMessageFlowPersistenceMode=" + this.incomingMessageFlowPersistenceMode +
                ", outgoingMessageFlowPersistenceMode=" + this.outgoingMessageFlowPersistenceMode +
                ", retainedMessagesPersistenceMode=" + this.retainedMessagesPersistenceMode +
                '}';
    }
}
