package i;

import com.google.inject.Singleton;
import io.netty.channel.local.LocalAddress;
import k.ClusterDiscovery;
import k.ClusterReplicates;
import k.ClusterTransport;
import m.ClusterFailureDetection;
import m.HeartbeatConfig;
import m.TcpHealthCheckConfig;
import n.ClusterReplicateClientSession;
import n.ClusterReplicateOutgoingMessageFlow;
import n.ClusterReplicateQueuedMessages;
import n.ClusterReplicateRetainedMessage;
import n.ClusterReplicateSubscriptions;
import n.ClusterReplicateTopicTree;
import org.apache.commons.lang3.RandomStringUtils;

@Singleton
public class ClusterConfigurationService {
    private boolean enabled;
    private final LocalAddress localAddress = new LocalAddress(RandomStringUtils.randomAlphabetic(10));
    private ClusterTransport transport;
    private ClusterDiscovery discovery;
    private ClusterReplicates replicates = new ClusterReplicates(
            new ClusterReplicateClientSession(),
            new ClusterReplicateQueuedMessages(),
            new ClusterReplicateOutgoingMessageFlow(),
            new ClusterReplicateRetainedMessage(),
            new ClusterReplicateSubscriptions(),
            new ClusterReplicateTopicTree());
    private ClusterFailureDetection failureDetection = new ClusterFailureDetection(
            new TcpHealthCheckConfig(),
            new HeartbeatConfig());

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public ClusterTransport getTransport() {
        return transport;
    }

    public void setTransport(ClusterTransport transport) {
        this.transport = transport;
    }

    public ClusterDiscovery getDiscovery() {
        return discovery;
    }

    public void setDiscovery(ClusterDiscovery discovery) {
        this.discovery = discovery;
    }

    public LocalAddress getLocalAddress() {
        return localAddress;
    }

    public ClusterReplicates getReplicates() {
        return this.replicates;
    }

    public void setReplicates(ClusterReplicates replicates) {
        this.replicates = replicates;
    }

    public ClusterFailureDetection getFailureDetection() {
        return this.failureDetection;
    }

    public void setFailureDetection(ClusterFailureDetection failureDetection) {
        this.failureDetection = failureDetection;
    }
}
