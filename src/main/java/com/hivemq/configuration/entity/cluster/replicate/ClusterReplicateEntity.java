package com.hivemq.configuration.entity.cluster.replicate;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlType(propOrder = {})
public abstract class ClusterReplicateEntity {
    @XmlElement(name = "replicate-count", defaultValue = "1")
    private final Integer replicateCount = 1;

    public Integer getReplicateCount() {
        return this.replicateCount;
    }
}
