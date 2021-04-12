package com.hivemq.configuration.entity.persistence;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlType(propOrder = {})
public class PersistenceConfigEntity {
    @XmlElement(name = "retained-messages")
    private RetainedMessagesPersistenceConfigEntity retainedMessages = new RetainedMessagesPersistenceConfigEntity();
    @XmlElement(name = "client-session")
    private ClientSessionPersistenceConfigEntity clientSessions = new ClientSessionPersistenceConfigEntity();
    @XmlElement(name = "message-flow")
    private MessageFlowPersistenceConfigEntity messageFlow = new MessageFlowPersistenceConfigEntity();

    public RetainedMessagesPersistenceConfigEntity getRetainedMessages() {
        return this.retainedMessages;
    }

    public ClientSessionPersistenceConfigEntity getClientSessions() {
        return this.clientSessions;
    }

    public MessageFlowPersistenceConfigEntity getMessageFlow() {
        return this.messageFlow;
    }
}
