package com.hivemq.configuration.entity.cluster.failuredetection;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlType(propOrder = {})
public class ClusterFailureDetectionEntity {
    @XmlElement(name = "tcp-health-check")
    private ClusterTcpHealthCheckEntity healthCheck = new ClusterTcpHealthCheckEntity();
    @XmlElement(name = "heartbeat")
    private ClusterHeartbeatEntity heartbeat = new ClusterHeartbeatEntity();

    public ClusterTcpHealthCheckEntity getHealthCheck() {
        return this.healthCheck;
    }

    public ClusterHeartbeatEntity getHeartbeat() {
        return this.heartbeat;
    }
}
