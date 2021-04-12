package com.hivemq.configuration.entity.cluster;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlType(propOrder = {})
public abstract class ClusterTransportEntity {
    @XmlElement(name = "bind-port", defaultValue = "8000", required = false)
    private Integer bindPort = 8000;
    @XmlElement(name = "bind-address")
    private String bindAddress = null;

    public int getBindPort() {
        return this.bindPort;
    }

    public String getBindAddress() {
        return this.bindAddress;
    }
}
