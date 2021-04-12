package t;

import av.InternalConfigurationService;
import av.Internals;
import cf.ClusterPluginDiscoveryService;
import com.google.common.net.InetAddresses;
import com.google.inject.Inject;
import com.hivemq.spi.exceptions.UnrecoverableException;
import d.CacheScoped;
import i.ClusterConfigurationService;
import k.ClusterDiscovery;
import k.ClusterTransport.Type;
import l.BroadcastDiscovery;
import l.DiscoveryNode;
import l.StaticDiscovery;
import m.HeartbeatConfig;
import m.TcpHealthCheckConfig;
import o.ClusterTcpTransport;
import o.ClusterUdpTransport;
import org.jgroups.PhysicalAddress;
import org.jgroups.protocols.BARRIER;
import org.jgroups.protocols.BPING;
import org.jgroups.protocols.FD;
import org.jgroups.protocols.FD_ALL;
import org.jgroups.protocols.FD_SOCK;
import org.jgroups.protocols.FRAG2;
import org.jgroups.protocols.MERGE3;
import org.jgroups.protocols.MFC;
import org.jgroups.protocols.PING;
import org.jgroups.protocols.RSVP;
import org.jgroups.protocols.TCP;
import org.jgroups.protocols.TCPPING;
import org.jgroups.protocols.UDP;
import org.jgroups.protocols.UFC;
import org.jgroups.protocols.UNICAST3;
import org.jgroups.protocols.VERIFY_SUSPECT;
import org.jgroups.protocols.pbcast.GMS;
import org.jgroups.protocols.pbcast.NAKACK2;
import org.jgroups.protocols.pbcast.STABLE;
import org.jgroups.protocols.pbcast.STATE_TRANSFER;
import org.jgroups.stack.IpAddress;
import org.jgroups.stack.Protocol;
import org.jgroups.stack.ProtocolStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Provider;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@CacheScoped
public class ClusterProtocolStackConfigurer {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterProtocolStackConfigurer.class);
    private final ClusterConfigurationService clusterConfigurationService;
    private final InternalConfigurationService internalConfigurationService;
    private final Provider<ClusterPluginDiscoveryService> clusterPluginDiscoveryServiceProvider;

    @Inject
    public ClusterProtocolStackConfigurer(
            ClusterConfigurationService clusterConfigurationService,
            InternalConfigurationService internalConfigurationService,
            Provider<ClusterPluginDiscoveryService> clusterPluginDiscoveryServiceProvider) {
        this.clusterConfigurationService = clusterConfigurationService;
        this.internalConfigurationService = internalConfigurationService;
        this.clusterPluginDiscoveryServiceProvider = clusterPluginDiscoveryServiceProvider;
    }

    public ProtocolStack getProtocolStack() {
        switch (this.clusterConfigurationService.getTransport().getType()) {
            case TCP:
                checkDiscoveryTypeForTcp();
                return getTcpProtocolStack();
            case UDP:
                checkDiscoveryTypeForUdp();
                return getUdpProtocolStack();
        }
        LOGGER.error("Unsupported configuration for cluster transport, please check your cluster configuration");
        throw new UnrecoverableException(false);
    }

    private void checkDiscoveryTypeForUdp() {
        if (this.clusterConfigurationService.getDiscovery().getType() == ClusterDiscovery.Type.STATIC) {
            LOGGER.error("Unsupported combination for cluster transport and discovery, please check your cluster configuration");
            throw new UnrecoverableException(false);
        }
    }

    private void checkDiscoveryTypeForTcp() {
        if (this.clusterConfigurationService.getDiscovery().getType() == ClusterDiscovery.Type.MULTICAST) {
            LOGGER.error("Unsupported combination for cluster transport and discovery, please check your cluster configuration");
            throw new UnrecoverableException(false);
        }
    }

    private ProtocolStack getUdpProtocolStack() {
        if (!(this.clusterConfigurationService.getTransport() instanceof ClusterUdpTransport)) {
            LOGGER.error("Unsupported configuration for cluster transport, please check your cluster configuration");
            throw new UnrecoverableException(false);
        }
        ClusterUdpTransport udpTransport = (ClusterUdpTransport) this.clusterConfigurationService.getTransport();
        ProtocolStack protocolStack = new ProtocolStack();
        List<Protocol> protocols = new ArrayList<>();
        protocols.add(new UDP()
                .setValue("bind_addr", udpTransport.getBindAddress() == null ? null : InetAddresses.forString(udpTransport.getBindAddress()))
                .setValue("bind_port", udpTransport.getBindPort())
                .setValue("ip_mcast", udpTransport.isMulticastEnabled())
                .setValue("mcast_port", udpTransport.getMulticastPort())
                .setValue("mcast_group_addr", InetAddresses.forString(udpTransport.getMulticastAddress()))
                .setValue("ip_ttl", this.internalConfigurationService.getInt(Internals.CLUSTER_UDP_IP_TTL))
                .setValue("tos", this.internalConfigurationService.getInt(Internals.CLUSTER_UDP_TOS))
                .setValue("ucast_recv_buf_size", this.internalConfigurationService.getInt(Internals.CLUSTER_UDP_UCAST_RECV_BUF_SIZE))
                .setValue("ucast_send_buf_size", this.internalConfigurationService.getInt(Internals.CLUSTER_UDP_UCAST_SEND_BUF_SIZE))
                .setValue("mcast_recv_buf_size", this.internalConfigurationService.getInt(Internals.CLUSTER_UDP_MCAST_RECV_BUF_SIZE))
                .setValue("mcast_send_buf_size", this.internalConfigurationService.getInt(Internals.CLUSTER_UDP_MCAST_SEND_BUF_SIZE))
                .setValue("max_bundle_size", this.internalConfigurationService.getInt(Internals.CLUSTER_UDP_MAX_BUNDLE_SIZE))
                .setValue("max_bundle_timeout", this.internalConfigurationService.getInt(Internals.CLUSTER_UDP_MAX_BUNDLE_TIMEOUT))
                .setValue("enable_diagnostics", this.internalConfigurationService.getBoolean(Internals.CLUSTER_UDP_ENABLE_DIAGNOSTICS))
                .setValue("thread_naming_pattern", "cl")
                .setValue("timer_type", "new3")
                .setValue("timer_min_threads", this.internalConfigurationService.getInt(Internals.CLUSTER_UDP_TIMER_MIN_THREADS))
                .setValue("timer_max_threads", this.internalConfigurationService.getInt(Internals.CLUSTER_UDP_TIMER_MAX_THREADS))
                .setValue("timer_keep_alive_time", this.internalConfigurationService.getInt(Internals.CLUSTER_UDP_TIMER_KEEP_ALIVE_TIME))
                .setValue("timer_queue_max_size", this.internalConfigurationService.getInt(Internals.CLUSTER_UDP_TIMER_QUEUE_MAX_SIZE))
                .setValue("thread_pool_enabled", this.internalConfigurationService.getBoolean(Internals.CLUSTER_UDP_THREAD_POOL_ENABLED))
                .setValue("thread_pool_min_threads", this.internalConfigurationService.getInt(Internals.CLUSTER_UDP_THREAD_POOL_MIN_THREADS))
                .setValue("thread_pool_max_threads", this.internalConfigurationService.getInt(Internals.CLUSTER_UDP_THREAD_POOL_MAX_THREADS))
                .setValue("thread_pool_keep_alive_time", this.internalConfigurationService.getInt(Internals.CLUSTER_UDP_THREAD_POOL_KEEP_ALIVE_TIME))
                .setValue("thread_pool_queue_enabled", this.internalConfigurationService.getBoolean(Internals.CLUSTER_UDP_THREAD_POOL_QUEUE_ENABLED))
                .setValue("thread_pool_queue_max_size", this.internalConfigurationService.getInt(Internals.CLUSTER_UDP_THREAD_POOL_QUEUE_MAX_SIZE))
                .setValue("thread_pool_rejection_policy", "discard")
                .setValue("oob_thread_pool_enabled", this.internalConfigurationService.getBoolean(Internals.CLUSTER_UDP_OOB_THREAD_POOL_ENABLED))
                .setValue("oob_thread_pool_min_threads", this.internalConfigurationService.getInt(Internals.CLUSTER_UDP_OOB_THREAD_POOL_MIN_THREADS))
                .setValue("oob_thread_pool_max_threads", this.internalConfigurationService.getInt(Internals.CLUSTER_UDP_OOB_THREAD_POOL_MAX_THREADS))
                .setValue("oob_thread_pool_keep_alive_time", this.internalConfigurationService.getInt(Internals.CLUSTER_UDP_OOB_THREAD_POOL_KEEP_ALIVE_TIME))
                .setValue("oob_thread_pool_queue_enabled", this.internalConfigurationService.getBoolean(Internals.CLUSTER_UDP_OOB_THREAD_POOL_QUEUE_ENABLED))
                .setValue("oob_thread_pool_queue_max_size", this.internalConfigurationService.getInt(Internals.CLUSTER_UDP_OOB_THREAD_POOL_QUEUE_MAX_SIZE))
                .setValue("oob_thread_pool_rejection_policy", "discard"));
        protocols.add(getDiscoveryProtocol());
        protocols.add(new MERGE3()
                .setValue("min_interval", this.internalConfigurationService.getInt(Internals.CLUSTER_UDP_MERGE3_MIN_INTERVAL))
                .setValue("max_interval", this.internalConfigurationService.getInt(Internals.CLUSTER_UDP_MERGE3_MAX_INTERVAL)));
        TcpHealthCheckConfig tcpHealthCheck = this.clusterConfigurationService.getFailureDetection().getTcpHealthCheckConfig();
        if (tcpHealthCheck.isEnabled()) {
            Protocol fdSock = new FD_SOCK()
                    .setValue("start_port", tcpHealthCheck.getBindPort())
                    .setValue("client_bind_port", this.internalConfigurationService.getInt(Internals.CLUSTER_UDP_FD_SOCK_CLIENT_BIND_PORT))
                    .setValue("port_range", tcpHealthCheck.getPortRange())
                    .setValue("num_tries", this.internalConfigurationService.getInt(Internals.CLUSTER_UDP_FD_SOCK_NUM_TRIES));
            if (tcpHealthCheck.getBindAddress() != null) {
                fdSock.setValue("bind_addr", InetAddresses.forString(tcpHealthCheck.getBindAddress()));
            }
            protocols.add(fdSock);
        }
        HeartbeatConfig heartbeat = this.clusterConfigurationService.getFailureDetection().getHeartbeatConfig();
        if (heartbeat.isEnabled()) {
            protocols.add(new FD_ALL()
                    .setValue("interval", heartbeat.getInterval())
                    .setValue("timeout", heartbeat.getTimeout()));
        }
        protocols.add(new VERIFY_SUSPECT()
                .setValue("timeout", this.internalConfigurationService.getInt(Internals.CLUSTER_UDP_VERIFY_SUSPECT_TIMEOUT)));
        protocols.add(new BARRIER());
        protocols.add(new NAKACK2()
                .setValue("xmit_interval", this.internalConfigurationService.getInt(Internals.CLUSTER_UDP_NACKACK2_XMIT_INTERVAL))
                .setValue("xmit_table_num_rows", this.internalConfigurationService.getInt(Internals.CLUSTER_UDP_NACKACK2_XMIT_TABLE_NUM_ROWS))
                .setValue("xmit_table_msgs_per_row", this.internalConfigurationService.getInt(Internals.CLUSTER_UDP_NACKACK2_XMIT_TABLE_MSGS_PER_ROW))
                .setValue("xmit_table_max_compaction_time", this.internalConfigurationService.getInt(Internals.CLUSTER_UDP_NACKACK2_XMIT_TABLE_MAX_COMPACTION_TIME))
                .setValue("max_msg_batch_size", this.internalConfigurationService.getInt(Internals.CLUSTER_UDP_NACKACK2_MAX_MSG_BATCH_SIZE))
                .setValue("use_mcast_xmit", this.internalConfigurationService.getBoolean(Internals.CLUSTER_UDP_NACKACK2_USE_MCAST_XMIT))
                .setValue("discard_delivered_msgs", this.internalConfigurationService.getBoolean(Internals.CLUSTER_UDP_NACKACK2_DISCARD_DELIVERED_MSGS)));
        protocols.add(new UNICAST3()
                .setValue("xmit_interval", this.internalConfigurationService.getInt(Internals.CLUSTER_UDP_UNICAST3_XMIT_INTERVAL))
                .setValue("xmit_table_num_rows", this.internalConfigurationService.getInt(Internals.CLUSTER_UDP_UNICAST3_XMIT_TABLE_NUM_ROWS))
                .setValue("xmit_table_msgs_per_row", this.internalConfigurationService.getInt(Internals.CLUSTER_UDP_UNICAST3_XMIT_TABLE_MSGS_PER_ROW))
                .setValue("xmit_table_max_compaction_time", this.internalConfigurationService.getInt(Internals.CLUSTER_UDP_UNICAST3_XMIT_TABLE_MAX_COMPACTION_TIME))
                .setValue("conn_expiry_timeout", this.internalConfigurationService.getInt(Internals.CLUSTER_UDP_UNICAST3_CONN_EXPIRY_TIMEOUT))
                .setValue("max_msg_batch_size", this.internalConfigurationService.getInt(Internals.CLUSTER_UDP_UNICAST3_MAX_MSG_BATCH_SIZE)));
        protocols.add(new STABLE()
                .setValue("stability_delay", this.internalConfigurationService.getInt(Internals.CLUSTER_UDP_STABLE_STABILITY_DELAY))
                .setValue("desired_avg_gossip", this.internalConfigurationService.getInt(Internals.CLUSTER_UDP_STABLE_DESIRED_AVG_GOSSIP))
                .setValue("max_bytes", this.internalConfigurationService.getLong(Internals.CLUSTER_UDP_STABLE_MAX_BYTES)));
        protocols.add(new GMS()
                .setValue("print_local_addr", this.internalConfigurationService.getBoolean(Internals.CLUSTER_UDP_GMS_PRINT_LOCAL_ADDR))
                .setValue("join_timeout", this.internalConfigurationService.getInt(Internals.CLUSTER_UDP_GMS_JOIN_TIMEOUT))
                .setValue("view_bundling", this.internalConfigurationService.getBoolean(Internals.CLUSTER_UDP_GMS_VIEW_BUNDLING)));
        protocols.add(new UFC()
                .setValue("max_credits", this.internalConfigurationService.getLong(Internals.CLUSTER_UDP_UFC_MAX_CREDITS))
                .setValue("min_threshold", this.internalConfigurationService.getDouble(Internals.CLUSTER_UDP_UFC_MIN_THRESHOLD)));
        protocols.add(new MFC()
                .setValue("max_credits", this.internalConfigurationService.getLong(Internals.CLUSTER_UDP_MFC_MAX_CREDITS))
                .setValue("min_threshold", this.internalConfigurationService.getDouble(Internals.CLUSTER_UDP_MFC_MIN_THRESHOLD)));
        protocols.add(new FRAG2()
                .setValue("frag_size", this.internalConfigurationService.getInt(Internals.CLUSTER_UDP_FRAG2_FRAG_SIZE)));
        protocols.add(new RSVP()
                .setValue("resend_interval", this.internalConfigurationService.getInt(Internals.CLUSTER_UDP_RSVP_RESEND_INTERVAL))
                .setValue("timeout", this.internalConfigurationService.getInt(Internals.CLUSTER_UDP_RSVP_TIMEOUT)));
        protocols.add(new STATE_TRANSFER());
        protocolStack.addProtocols(protocols);
        return protocolStack;
    }

    private ProtocolStack getTcpProtocolStack() {
        if (!(this.clusterConfigurationService.getTransport() instanceof ClusterTcpTransport)) {
            LOGGER.error("Unsupported configuration for cluster transport, please check your cluster configuration");
            throw new UnrecoverableException(false);
        }
        ClusterTcpTransport tcpTransport = (ClusterTcpTransport) this.clusterConfigurationService.getTransport();
        ProtocolStack protocolStack = new ProtocolStack();
        List<Protocol> protocols = new ArrayList<>();
        TCP tcp = new TCP();
        if (tcpTransport.getBindAddress() != null) {
            tcp.setValue("bind_addr", InetAddresses.forString(tcpTransport.getBindAddress()));
        }
        tcp.setValue("bind_port", tcpTransport.getBindPort());
        if (tcpTransport.getClientBindAddress() != null) {
            tcp.setValue("client_bind_addr", InetAddresses.forString(tcpTransport.getClientBindAddress()));
        }
        if (tcpTransport.getClientBindPort() != 0) {
            tcp.setValue("client_bind_port", tcpTransport.getClientBindPort());
        }
        tcp.setValue("recv_buf_size", this.internalConfigurationService.getInt(Internals.CLUSTER_TCP_RECV_BUF_SIZE))
                .setValue("send_buf_size", this.internalConfigurationService.getInt(Internals.CLUSTER_TCP_SEND_BUF_SIZE))
                .setValue("max_bundle_size", this.internalConfigurationService.getInt(Internals.CLUSTER_TCP_MAX_BUNDLE_SIZE))
                .setValue("max_bundle_timeout", this.internalConfigurationService.getInt(Internals.CLUSTER_TCP_MAX_BUNDLE_TIMEOUT))
                .setValue("sock_conn_timeout", this.internalConfigurationService.getInt(Internals.CLUSTER_TCP_SOCK_CONN_TIMEOUT))
                .setValue("timer_type", "new3")
                .setValue("timer_min_threads", this.internalConfigurationService.getInt(Internals.CLUSTER_TCP_TIMER_MIN_THREADS))
                .setValue("timer_max_threads", this.internalConfigurationService.getInt(Internals.CLUSTER_TCP_TIMER_MAX_THREADS))
                .setValue("timer_keep_alive_time", this.internalConfigurationService.getInt(Internals.CLUSTER_TCP_TIMER_KEEP_ALIVE_TIME))
                .setValue("timer_queue_max_size", this.internalConfigurationService.getInt(Internals.CLUSTER_TCP_TIMER_QUEUE_MAX_SIZE))
                .setValue("thread_pool_enabled", this.internalConfigurationService.getBoolean(Internals.CLUSTER_TCP_THREAD_POOL_ENABLED))
                .setValue("thread_pool_min_threads", this.internalConfigurationService.getInt(Internals.CLUSTER_TCP_THREAD_POOL_MIN_THREADS))
                .setValue("thread_pool_max_threads", this.internalConfigurationService.getInt(Internals.CLUSTER_TCP_THREAD_POOL_MAX_THREADS))
                .setValue("thread_pool_keep_alive_time", this.internalConfigurationService.getInt(Internals.CLUSTER_TCP_THREAD_POOL_KEEP_ALIVE_TIME))
                .setValue("thread_pool_queue_enabled", this.internalConfigurationService.getBoolean(Internals.CLUSTER_TCP_THREAD_POOL_QUEUE_ENABLED))
                .setValue("thread_pool_queue_max_size", this.internalConfigurationService.getInt(Internals.CLUSTER_TCP_THREAD_POOL_QUEUE_MAX_SIZE))
                .setValue("thread_pool_rejection_policy", "discard")
                .setValue("oob_thread_pool_enabled", this.internalConfigurationService.getBoolean(Internals.CLUSTER_TCP_OOB_THREAD_POOL_ENABLED))
                .setValue("oob_thread_pool_min_threads", this.internalConfigurationService.getInt(Internals.CLUSTER_TCP_OOB_THREAD_POOL_MIN_THREADS))
                .setValue("oob_thread_pool_max_threads", this.internalConfigurationService.getInt(Internals.CLUSTER_TCP_OOB_THREAD_POOL_MAX_THREADS))
                .setValue("oob_thread_pool_keep_alive_time", this.internalConfigurationService.getInt(Internals.CLUSTER_TCP_OOB_THREAD_POOL_KEEP_ALIVE_TIME))
                .setValue("oob_thread_pool_queue_enabled", this.internalConfigurationService.getBoolean(Internals.CLUSTER_TCP_OOB_THREAD_POOL_QUEUE_ENABLED))
                .setValue("oob_thread_pool_queue_max_size", this.internalConfigurationService.getInt(Internals.CLUSTER_TCP_OOB_THREAD_POOL_QUEUE_MAX_SIZE))
                .setValue("oob_thread_pool_rejection_policy", "discard");
        protocols.add(tcp);
        protocols.add(getDiscoveryProtocol());
        protocols.add(new MERGE3()
                .setValue("min_interval", this.internalConfigurationService.getInt(Internals.CLUSTER_TCP_MERGE3_MIN_INTERVAL))
                .setValue("max_interval", this.internalConfigurationService.getInt(Internals.CLUSTER_TCP_MERGE3_MAX_INTERVAL)));
        TcpHealthCheckConfig tcpHealthCheckConfig = this.clusterConfigurationService.getFailureDetection().getTcpHealthCheckConfig();
        if (tcpHealthCheckConfig.isEnabled()) {
            Protocol fdSock = new FD_SOCK()
                    .setValue("start_port", tcpHealthCheckConfig.getBindPort())
                    .setValue("client_bind_port", this.internalConfigurationService.getInt(Internals.CLUSTER_TCP_FD_SOCK_CLIENT_BIND_PORT))
                    .setValue("port_range", tcpHealthCheckConfig.getPortRange())
                    .setValue("num_tries", this.internalConfigurationService.getInt(Internals.CLUSTER_TCP_FD_SOCK_NUM_TRIES));
            if (tcpHealthCheckConfig.getBindAddress() != null) {
                fdSock.setValue("bind_addr", InetAddresses.forString(tcpHealthCheckConfig.getBindAddress()));
            }
            protocols.add(fdSock);
        }
        HeartbeatConfig heartbeatConfig = this.clusterConfigurationService.getFailureDetection().getHeartbeatConfig();
        if (heartbeatConfig.isEnabled()) {
            protocols.add(new FD()
                    .setValue("timeout", heartbeatConfig.getTimeout())
                    .setValue("max_tries", getMaxTries(heartbeatConfig)));
        }
        protocols.add(new VERIFY_SUSPECT()
                .setValue("timeout", this.internalConfigurationService.getInt(Internals.CLUSTER_TCP_VERIFY_SUSPECT_TIMEOUT)));
        protocols.add(new BARRIER());
        protocols.add(new NAKACK2()
                .setValue("use_mcast_xmit", this.internalConfigurationService.getBoolean(Internals.CLUSTER_TCP_NAKACK2_USE_MCAST_XMIT))
                .setValue("discard_delivered_msgs", this.internalConfigurationService.getBoolean(Internals.CLUSTER_TCP_NAKACK2_DISCARD_DELIVERED_MSGS)));
        protocols.add(new UNICAST3());
        protocols.add(new STABLE()
                .setValue("stability_delay", this.internalConfigurationService.getInt(Internals.CLUSTER_TCP_STABLE_STABILITY_DELAY))
                .setValue("desired_avg_gossip", this.internalConfigurationService.getInt(Internals.CLUSTER_TCP_STABLE_DESIRED_AVG_GOSSIP))
                .setValue("max_bytes", this.internalConfigurationService.getLong(Internals.CLUSTER_TCP_STABLE_MAX_BYTES)));
        protocols.add(new GMS()
                .setValue("print_local_addr", this.internalConfigurationService.getBoolean(Internals.CLUSTER_TCP_GMS_PRINT_LOCAL_ADDR))
                .setValue("join_timeout", this.internalConfigurationService.getInt(Internals.CLUSTER_TCP_GMS_JOIN_TIMEOUT))
                .setValue("view_bundling", this.internalConfigurationService.getBoolean(Internals.CLUSTER_TCP_GMS_VIEW_BUNDLING)));
        protocols.add(new MFC()
                .setValue("max_credits", this.internalConfigurationService.getLong(Internals.CLUSTER_TCP_MFC_MAX_CREDITS))
                .setValue("min_threshold", this.internalConfigurationService.getDouble(Internals.CLUSTER_TCP_MFC_MIN_THRESHOLD)));
        protocols.add(new FRAG2()
                .setValue("frag_size", this.internalConfigurationService.getInt(Internals.CLUSTER_TCP_FRAG2_FRAG_SIZE)));
        protocols.add(new STATE_TRANSFER());
        protocolStack.addProtocols(protocols);
        return protocolStack;
    }

    private int getMaxTries(HeartbeatConfig heartbeat) {
        if (heartbeat.getInterval() == 0) {
            return 0;
        }
        return (int) Math.ceil(heartbeat.getTimeout() / heartbeat.getInterval());
    }


    private Protocol getDiscoveryProtocol() {
        ClusterDiscovery discovery = this.clusterConfigurationService.getDiscovery();
        switch (discovery.getType()) {
            case STATIC:
                if (this.clusterConfigurationService.getTransport().getType() == Type.UDP) {
                    LOGGER.error("Unsupported configuration for cluster discovery, STATIC discovery is not supported for UDP transport, please check your cluster configuration");
                    throw new UnrecoverableException(false);
                }
                StaticDiscovery staticDiscovery = (StaticDiscovery) discovery;
                TCPPING tcpping = new TCPPING();
                tcpping.setValue("async_discovery", true)
                        .setValue("port_range", 0)
                        .setValue("initial_hosts", getNodePhysicalAddresses(staticDiscovery.getNodes()));
                return tcpping;
            case MULTICAST:
                if (this.clusterConfigurationService.getTransport().getType() == Type.TCP) {
                    LOGGER.error("Unsupported configuration for cluster discovery, MULTICAST discovery is not supported for TCP transport, please check your cluster configuration");
                    throw new UnrecoverableException(false);
                }
                return new PING();
            case BROADCAST:
                BroadcastDiscovery broadcastDiscovery = (BroadcastDiscovery) discovery;
                BPING bping = new BPING();
                bping.setValue("bind_port", broadcastDiscovery.getPort());
                bping.setValue("port_range", broadcastDiscovery.getPortRange());
                bping.setValue("dest", broadcastDiscovery.getBroadcastAddress());
                return bping;
            case PLUGIN:
                return new PluginPING(this.clusterPluginDiscoveryServiceProvider.get());
        }
        LOGGER.error("Unsupported configuration for cluster discovery, please check your cluster configuration");
        throw new UnrecoverableException(false);
    }

    private List<PhysicalAddress> getNodePhysicalAddresses(List<DiscoveryNode> nodes) {
        return nodes.stream()
                .map(node -> {
                    try {
                        return new IpAddress(node.getHost(), node.getPort());
                    } catch (UnknownHostException e) {
                        LOGGER.error("Unknown host {} for cluster configuration", node.getHost(), e);
                        throw new UnrecoverableException(false);
                    }
                })
                .collect(Collectors.toList());
    }
}
