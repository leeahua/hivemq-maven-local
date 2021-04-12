package com.hivemq.configuration.entity.cluster.replicate;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlType(propOrder = {})
public class ClusterReplicateOutgoingMessageFlowEntity
        extends ClusterReplicateEntity {
    @XmlElement(name = "replication-interval", defaultValue = "1000")
    private Integer replicationInterval = 1000;

    public Integer getReplicationInterval() {
        return this.replicationInterval;
    }
}
