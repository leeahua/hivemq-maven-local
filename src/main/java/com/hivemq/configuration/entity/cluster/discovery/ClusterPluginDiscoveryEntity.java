package com.hivemq.configuration.entity.cluster.discovery;

import com.hivemq.configuration.entity.cluster.ClusterDiscoveryEntity;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlType(propOrder = {})
public class ClusterPluginDiscoveryEntity
        extends ClusterDiscoveryEntity {
    @XmlElement(name = "reload-interval", defaultValue = "60", required = false)
    private Integer interval = 60;

    public Integer getInterval() {
        return this.interval;
    }
}
