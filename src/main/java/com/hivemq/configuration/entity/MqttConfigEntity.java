package com.hivemq.configuration.entity;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlType(propOrder = {})
public class MqttConfigEntity {
    @XmlElement(name = "max-client-id-length", defaultValue = "65535")
    private Integer maxClientIdLength = 65535;
    @XmlElement(name = "retry-interval", defaultValue = "10")
    private Integer retryInterval = 10;
    @XmlElement(name = "no-connect-packet-idle-timeout-millis", defaultValue = "10000")
    private Long noConnectIdleTimeoutMillis = 10000L;
    @XmlElement(name = "max-queued-messages", defaultValue = "1000")
    private Long maxQueuedMessages = 1000L;

    public int getMaxClientIdLength() {
        return this.maxClientIdLength;
    }

    public int getRetryInterval() {
        return this.retryInterval;
    }

    public long getNoConnectIdleTimeoutMillis() {
        return this.noConnectIdleTimeoutMillis;
    }

    public Long getMaxQueuedMessages() {
        return this.maxQueuedMessages;
    }
}
