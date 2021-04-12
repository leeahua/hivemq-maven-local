package com.hivemq.configuration.entity;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlType(propOrder = {})
public class ThrottlingEntity {
    @XmlElement(name = "max-connections", defaultValue = "-1")
    private Long maxConnections = Long.valueOf(-1L);
    @XmlElement(name = "max-message-size", defaultValue = "268435456")
    private Integer maxMessageSize = 268435456;
    @XmlElement(name = "outgoing-limit", defaultValue = "0")
    private Long outgoingLimit = Long.valueOf(0L);
    @XmlElement(name = "incoming-limit", defaultValue = "0")
    private Long incomingLimit = Long.valueOf(0L);

    public long getMaxConnections() {
        return this.maxConnections.longValue();
    }

    public int getMaxMessageSize() {
        return this.maxMessageSize;
    }

    public long getOutgoingLimit() {
        return this.outgoingLimit.longValue();
    }

    public long getIncomingLimit() {
        return this.incomingLimit.longValue();
    }
}
