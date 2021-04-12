package com.hivemq.configuration.entity.cluster.failuredetection;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlType(propOrder = {})
public class ClusterTcpHealthCheckEntity {
    @XmlElement(name = "enabled", defaultValue = "true", required = false)
    private Boolean enabled = Boolean.TRUE;
    @XmlElement(name = "bind-address", required = false)
    private String bindAddress = null;
    @XmlElement(name = "bind-port", defaultValue = "0", required = false)
    private Integer bindPort = 0;
    @XmlElement(name = "port-range", defaultValue = "50", required = false)
    private Integer portRange = 50;

    public Boolean getEnabled() {
        return this.enabled;
    }

    public String getBindAddress() {
        return this.bindAddress;
    }

    public int getBindPort() {
        return this.bindPort;
    }

    public int getPortRange() {
        return this.portRange;
    }
}
