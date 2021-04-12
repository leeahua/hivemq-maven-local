package t;

import cf.ClusterPluginDiscoveryService;
import com.hivemq.spi.annotations.NotNull;
import com.hivemq.spi.callback.cluster.ClusterNodeAddress;
import org.jgroups.Address;
import org.jgroups.Event;
import org.jgroups.Message;
import org.jgroups.PhysicalAddress;
import org.jgroups.conf.ClassConfigurator;
import org.jgroups.protocols.Discovery;
import org.jgroups.protocols.PingData;
import org.jgroups.protocols.PingHeader;
import org.jgroups.stack.IpAddress;
import org.jgroups.util.Responses;
import org.jgroups.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.UnknownHostException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class PluginPING extends Discovery {
    private static final Logger LOGGER = LoggerFactory.getLogger(PluginPING.class);
    public static final int ID = 1883;
    private final ClusterPluginDiscoveryService clusterPluginDiscoveryService;

    public PluginPING(ClusterPluginDiscoveryService clusterPluginDiscoveryService) {
        this.clusterPluginDiscoveryService = clusterPluginDiscoveryService;
    }

    public void init() throws Exception {
        super.init();
        try {
            this.id = ID;
            this.async_discovery_use_separate_thread_per_request = true;
            ClassConfigurator.addProtocol((short) ID, PluginPING.class);
        } catch (IllegalArgumentException e) {
        }
    }

    public boolean isDynamic() {
        return true;
    }

    protected void findMembers(List<Address> members, boolean initial_discovery, Responses responses) {
        PhysicalAddress physicalAddress = (PhysicalAddress) down(new Event(Event.GET_PHYSICAL_ADDRESS, this.local_addr));
        PingData pingData = new PingData(this.local_addr, false, UUID.get(this.local_addr), physicalAddress);
        PingHeader pingHeader = new PingHeader((byte) 1).clusterName(this.cluster_name);
        List<PhysicalAddress> physicalAddresses = getPhysicalAddresses();
        physicalAddresses.stream()
                .filter(Objects::nonNull)
                .filter(pa -> !pa.equals(physicalAddress))
                .forEach(pa -> {
                    Message message = new Message(pa)
                            .setFlag(Message.Flag.INTERNAL, Message.Flag.DONT_BUNDLE, Message.Flag.OOB)
                            .putHeader(this.id, pingHeader)
                            .setBuffer(marshal(pingData));
                    if (this.async_discovery_use_separate_thread_per_request) {
                        this.timer.execute(() -> {
                            LOGGER.trace("{}: sending cluster discovery request to {}", this.local_addr, message.getDest());
                            this.down_prot.down(new Event(1, message));
                        });
                    } else {
                        LOGGER.trace("{}: sending cluster discovery request to {}", this.local_addr, message.getDest());
                        this.down_prot.down(new Event(1, message));
                    }
                });
    }


    @NotNull
    private List<PhysicalAddress> getPhysicalAddresses() {
        return this.clusterPluginDiscoveryService.getClusterNodeAddresses().stream()
                .map(nodeAddress -> {
                    try {
                        return getPhysicalAddress(nodeAddress);
                    } catch (UnknownHostException e) {
                        LOGGER.warn("Host {} for cluster node not found, ignoring this address");
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }


    private PhysicalAddress getPhysicalAddress(ClusterNodeAddress nodeAddress) throws UnknownHostException {
        return new IpAddress(nodeAddress.getHost(), nodeAddress.getPort());
    }
}
