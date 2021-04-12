package com.hivemq.configuration.entity.cluster.failuredetection;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlType(propOrder = {})
public class ClusterHeartbeatEntity {
    public static final int INTERVAL_TCP_DEFAULT = 3000;
    public static final int INTERVAL_UDP_DEFAULT = 8000;
    public static final int TIMEOUT_TCP_DEFAULT = 9000;
    public static final int TIMEOUT_UDP_DEFAULT = 40000;
    @XmlElement(name = "enabled", defaultValue = "true")
    private Boolean enabled = Boolean.TRUE;
    @XmlElement(name = "interval", required = false)
    private Integer interval = null;
    @XmlElement(name = "timeout", required = false)
    private Integer timeout = null;

    public Boolean getEnabled() {
        return this.enabled;
    }

    public Integer getInterval() {
        return this.interval;
    }

    public Integer getTimeout() {
        return this.timeout;
    }
}
