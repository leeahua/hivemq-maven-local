package com.hivemq.configuration.entity.cluster.transport;

import com.hivemq.configuration.entity.cluster.ClusterTransportEntity;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlType(propOrder = {})
public class ClusterUdpTransportEntity
        extends ClusterTransportEntity {
    @XmlElement(name = "multicast-enabled", defaultValue = "true", required = false)
    private Boolean multicastEnabled = true;
    @XmlElement(name = "multicast-port", defaultValue = "45588", required = false)
    private Integer multicastPort = 45588;
    @XmlElement(name = "multicast-address", defaultValue = "228.8.8.8")
    private String multicastAddress = "228.8.8.8";

    public Boolean isMulticastEnabled() {
        return this.multicastEnabled;
    }

    public Integer getMulticastPort() {
        return this.multicastPort;
    }

    public String getMulticastAddress() {
        return this.multicastAddress;
    }
}
