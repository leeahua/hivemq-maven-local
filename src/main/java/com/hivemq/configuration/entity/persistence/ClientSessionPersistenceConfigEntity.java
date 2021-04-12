package com.hivemq.configuration.entity.persistence;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;

@XmlType(propOrder = {})
public class ClientSessionPersistenceConfigEntity {
    @XmlElement(name = "general")
    private ClientSessionGeneralConfigEntity general = new ClientSessionGeneralConfigEntity();
    @XmlElement(name = "subscriptions")
    private ClientSessionSubscriptionsConfigEntity subscriptions = new ClientSessionSubscriptionsConfigEntity();
    @XmlElement(name = "queued-messages")
    private ClientSessionQueuedMessagesConfigEntity queuedMessages = new ClientSessionQueuedMessagesConfigEntity();

    public ClientSessionGeneralConfigEntity getGeneral() {
        return this.general;
    }

    public ClientSessionSubscriptionsConfigEntity getSubscriptions() {
        return this.subscriptions;
    }

    public ClientSessionQueuedMessagesConfigEntity getQueuedMessages() {
        return this.queuedMessages;
    }

    @XmlType(propOrder = {})
    public static class ClientSessionQueuedMessagesConfigEntity
            extends PersistenceEntity {
        @XmlElement(name = "mode", defaultValue = "file")
        private PersistenceModeEntity persistenceMode = PersistenceModeEntity.FILE;
        @XmlElement(name = "max-queued-messages", defaultValue = "1000")
        private long maxQueuedMessages = 1000L;
        @XmlElement(name = "queued-messages-strategy", defaultValue = "discard")
        private QueuedMessagesStrategy queuedMessagesStrategy = QueuedMessagesStrategy.DISCARD;

        public PersistenceModeEntity getPersistenceMode() {
            return this.persistenceMode;
        }

        public long getMaxQueuedMessages() {
            return this.maxQueuedMessages;
        }

        public QueuedMessagesStrategy getQueuedMessagesStrategy() {
            return this.queuedMessagesStrategy;
        }

        @XmlEnum
        @XmlType(name = "queued-messages-strategy")
        public static enum QueuedMessagesStrategy {
            DISCARD_OLDEST, DISCARD;

            private QueuedMessagesStrategy() {
            }
        }
    }

    @XmlType(propOrder = {})
    public static class ClientSessionSubscriptionsConfigEntity
            extends PersistenceEntity {
        @XmlElement(name = "mode", defaultValue = "file")
        private PersistenceModeEntity persistenceMode = PersistenceModeEntity.FILE;

        public PersistenceModeEntity getPersistenceMode() {
            return this.persistenceMode;
        }
    }

    @XmlType(propOrder = {})
    public static class ClientSessionGeneralConfigEntity
            extends PersistenceEntity {
        @XmlElement(name = "mode", defaultValue = "file")
        private PersistenceModeEntity persistenceMode = PersistenceModeEntity.FILE;

        public PersistenceModeEntity getPersistenceMode() {
            return this.persistenceMode;
        }
    }
}
