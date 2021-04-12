package com.hivemq.configuration.entity;

import com.hivemq.configuration.entity.cluster.ClusterDiscoveryEntity;
import com.hivemq.configuration.entity.cluster.ClusterReplicatesEntity;
import com.hivemq.configuration.entity.cluster.ClusterTransportEntity;
import com.hivemq.configuration.entity.cluster.failuredetection.ClusterFailureDetectionEntity;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlType;
import java.util.ArrayList;
import java.util.List;

@XmlType(propOrder = {})
public class ClusterConfigEntity {
    @XmlElement(name = "enabled", defaultValue = "false")
    private boolean enabled = false;
    @XmlElementWrapper(name = "transport")
    @XmlElements({@XmlElement(name = "tcp", type = com.hivemq.configuration.entity.cluster.transport.ClusterTcpTransportEntity.class), @XmlElement(name = "udp", type = com.hivemq.configuration.entity.cluster.transport.ClusterUdpTransportEntity.class)})
    private List<ClusterTransportEntity> transport = new ArrayList();
    @XmlElementWrapper(name = "discovery")
    @XmlElements({@XmlElement(name = "static", type = com.hivemq.configuration.entity.cluster.discovery.ClusterStaticDiscoveryEntity.class), @XmlElement(name = "plugin", type = com.hivemq.configuration.entity.cluster.discovery.ClusterPluginDiscoveryEntity.class), @XmlElement(name = "multicast", type = com.hivemq.configuration.entity.cluster.discovery.ClusterMulticastDiscoveryEntity.class), @XmlElement(name = "broadcast", type = com.hivemq.configuration.entity.cluster.discovery.ClusterBroadcastDiscoveryEntity.class)})
    private List<ClusterDiscoveryEntity> discovery = new ArrayList();
    @XmlElement(name = "replicates")
    private ClusterReplicatesEntity replicates = new ClusterReplicatesEntity();
    @XmlElement(name = "failure-detection")
    private ClusterFailureDetectionEntity failureDetection = new ClusterFailureDetectionEntity();

    public boolean isEnabled() {
        return this.enabled;
    }

    public List<ClusterTransportEntity> getTransport() {
        return this.transport;
    }

    public List<ClusterDiscoveryEntity> getDiscovery() {
        return this.discovery;
    }

    public ClusterReplicatesEntity getReplicates() {
        return this.replicates;
    }

    public ClusterFailureDetectionEntity getFailureDetection() {
        return this.failureDetection;
    }
}
