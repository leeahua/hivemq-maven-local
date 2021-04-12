package com.hivemq.configuration.entity.cluster.discovery;

import com.hivemq.configuration.entity.cluster.ClusterDiscoveryEntity;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlType(propOrder = {})
public class ClusterBroadcastDiscoveryEntity
        extends ClusterDiscoveryEntity {
    @XmlElement(name = "port", defaultValue = "8555", required = false)
    private Integer port = 8555;
    @XmlElement(name = "broadcast-address", defaultValue = "255.255.255.255")
    private String broadcastAddress = "255.255.255.255";
    @XmlElement(name = "port-range", defaultValue = "0", required = false)
    private Integer portRange = 0;

    public int getPort() {
        return this.port;
    }

    public String getBroadcastAddress() {
        return this.broadcastAddress;
    }

    public int getPortRange() {
        return this.portRange;
    }
}
