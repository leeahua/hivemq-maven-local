package m;

import com.google.common.base.Preconditions;

public class ClusterFailureDetection {
    private final TcpHealthCheckConfig tcpHealthCheckConfig;
    private final HeartbeatConfig heartbeatConfig;

    public ClusterFailureDetection(TcpHealthCheckConfig tcpHealthCheckConfig,
                                   HeartbeatConfig heartbeatConfig) {
        Preconditions.checkNotNull(tcpHealthCheckConfig, "TcpHealthCheckConfig cannot be null");
        Preconditions.checkNotNull(heartbeatConfig, "HeartbeatConfig cannot be null");
        this.tcpHealthCheckConfig = tcpHealthCheckConfig;
        this.heartbeatConfig = heartbeatConfig;
    }

    public TcpHealthCheckConfig getTcpHealthCheckConfig() {
        return tcpHealthCheckConfig;
    }

    public HeartbeatConfig getHeartbeatConfig() {
        return heartbeatConfig;
    }
}
