package com.hivemq.configuration.entity.cluster.discovery;

import com.hivemq.configuration.entity.cluster.ClusterDiscoveryEntity;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import java.util.ArrayList;
import java.util.List;

@XmlType(propOrder = {})
public class ClusterStaticDiscoveryEntity
        extends ClusterDiscoveryEntity {
    @XmlElement(name = "node", type = ClusterStaticDiscoveryNodeEntity.class)
    private List<ClusterStaticDiscoveryNodeEntity> nodes = new ArrayList();

    public List<ClusterStaticDiscoveryNodeEntity> getNodes() {
        return this.nodes;
    }

    public static class ClusterStaticDiscoveryNodeEntity {
        @XmlElement(name = "host", required = true)
        private String host;
        @XmlElement(name = "port", required = true)
        private int port;

        public String getHost() {
            return this.host;
        }

        public int getPort() {
            return this.port;
        }
    }
}
