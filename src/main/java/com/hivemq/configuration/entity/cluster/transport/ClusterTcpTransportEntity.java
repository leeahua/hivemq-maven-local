package com.hivemq.configuration.entity.cluster.transport;

import com.hivemq.configuration.entity.cluster.ClusterTransportEntity;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlType(propOrder = {})
public class ClusterTcpTransportEntity
        extends ClusterTransportEntity {
    @XmlElement(name = "client-bind-address", required = false)
    private String clientBindAddress = null;
    @XmlElement(name = "client-bind-port", defaultValue = "0", required = false)
    private Integer clientBindPort = 0;

    public String getClientBindAddress() {
        return this.clientBindAddress;
    }

    public int getClientBindPort() {
        return this.clientBindPort;
    }
}
