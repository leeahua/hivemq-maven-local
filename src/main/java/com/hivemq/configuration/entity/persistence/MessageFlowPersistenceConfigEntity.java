package com.hivemq.configuration.entity.persistence;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlType(propOrder = {})
public class MessageFlowPersistenceConfigEntity {
    @XmlElement(name = "incoming")
    private MessageFlowIncomingConfigEntity incoming = new MessageFlowIncomingConfigEntity();
    @XmlElement(name = "outgoing")
    private MessageFlowOutgoingConfigEntity outgoing = new MessageFlowOutgoingConfigEntity();

    public MessageFlowIncomingConfigEntity getIncoming() {
        return this.incoming;
    }

    public MessageFlowOutgoingConfigEntity getOutgoing() {
        return this.outgoing;
    }

    @XmlType(propOrder = {})
    public static class MessageFlowOutgoingConfigEntity
            extends PersistenceEntity {
        @XmlElement(name = "mode", defaultValue = "file")
        private PersistenceModeEntity persistenceMode = PersistenceModeEntity.FILE;

        public PersistenceModeEntity getPersistenceMode() {
            return this.persistenceMode;
        }
    }

    @XmlType(propOrder = {})
    public static class MessageFlowIncomingConfigEntity
            extends PersistenceEntity {
        @XmlElement(name = "mode", defaultValue = "in-memory")
        private PersistenceModeEntity persistenceMode = PersistenceModeEntity.IN_MEMORY;

        public PersistenceModeEntity getPersistenceMode() {
            return this.persistenceMode;
        }
    }
}
