package au;

import av.InternalConfigurationService;
import av.PersistenceConfig;
import av.PersistenceConfig.GCType;
import av.PersistenceConfigurationService;
import av.PersistenceConfigurationService.PersistenceMode;
import av.PersistenceConfigurationService.QueuedMessagesStrategy;
import av.RestConfigurationService;
import av.SharedSubscriptionsConfigurationService;
import aw.PersistenceConfigImpl;
import cb1.EnvironmentVariableReplacer;
import com.google.common.collect.ImmutableList;
import com.hivemq.configuration.entity.ClientAuthenticationModeEntity;
import com.hivemq.configuration.entity.HiveMQConfigEntity;
import com.hivemq.configuration.entity.InternalConfigEntity;
import com.hivemq.configuration.entity.ListenerEntity;
import com.hivemq.configuration.entity.MqttConfigEntity;
import com.hivemq.configuration.entity.SharedSubscriptionsEntity;
import com.hivemq.configuration.entity.TCPListenerEntity;
import com.hivemq.configuration.entity.TLSEntity;
import com.hivemq.configuration.entity.ThrottlingEntity;
import com.hivemq.configuration.entity.TlsTCPListenerEntity;
import com.hivemq.configuration.entity.TlsWebsocketListenerEntity;
import com.hivemq.configuration.entity.WebsocketListenerEntity;
import com.hivemq.configuration.entity.cluster.ClusterDiscoveryEntity;
import com.hivemq.configuration.entity.cluster.ClusterReplicatesEntity;
import com.hivemq.configuration.entity.cluster.ClusterTransportEntity;
import com.hivemq.configuration.entity.cluster.discovery.ClusterBroadcastDiscoveryEntity;
import com.hivemq.configuration.entity.cluster.discovery.ClusterMulticastDiscoveryEntity;
import com.hivemq.configuration.entity.cluster.discovery.ClusterPluginDiscoveryEntity;
import com.hivemq.configuration.entity.cluster.discovery.ClusterStaticDiscoveryEntity;
import com.hivemq.configuration.entity.cluster.failuredetection.ClusterFailureDetectionEntity;
import com.hivemq.configuration.entity.cluster.failuredetection.ClusterHeartbeatEntity;
import com.hivemq.configuration.entity.cluster.transport.ClusterTcpTransportEntity;
import com.hivemq.configuration.entity.cluster.transport.ClusterUdpTransportEntity;
import com.hivemq.configuration.entity.persistence.ClientSessionPersistenceConfigEntity;
import com.hivemq.configuration.entity.persistence.GCTypeEntity;
import com.hivemq.configuration.entity.persistence.MessageFlowPersistenceConfigEntity;
import com.hivemq.configuration.entity.persistence.PersistenceConfigConfigurationEntity;
import com.hivemq.configuration.entity.persistence.PersistenceConfigEntity;
import com.hivemq.configuration.entity.persistence.PersistenceModeEntity;
import com.hivemq.configuration.entity.rest.HttpRestListenerEntity;
import com.hivemq.spi.annotations.Nullable;
import com.hivemq.spi.exceptions.UnrecoverableException;
import com.hivemq.spi.services.configuration.GeneralConfigurationService;
import com.hivemq.spi.services.configuration.MqttConfigurationService;
import com.hivemq.spi.services.configuration.ThrottlingConfigurationService;
import com.hivemq.spi.services.configuration.entity.Listener;
import com.hivemq.spi.services.configuration.entity.TcpListener;
import com.hivemq.spi.services.configuration.entity.Tls;
import com.hivemq.spi.services.configuration.entity.TlsTcpListener;
import com.hivemq.spi.services.configuration.entity.TlsWebsocketListener;
import com.hivemq.spi.services.configuration.entity.WebsocketListener;
import com.hivemq.spi.services.configuration.listener.ListenerConfigurationService;
import com.hivemq.spi.services.rest.listener.HttpListener;
import i.ClusterConfigurationService;
import k.ClusterDiscovery;
import k.ClusterReplicates;
import k.ClusterTransport;
import l.BroadcastDiscovery;
import l.DiscoveryNode;
import l.MulticastDiscovery;
import l.PluginDiscovery;
import l.StaticDiscovery;
import m.ClusterFailureDetection;
import m.HeartbeatConfig;
import m.TcpHealthCheckConfig;
import n.ClusterReplicateClientSession;
import n.ClusterReplicateOutgoingMessageFlow;
import n.ClusterReplicateQueuedMessages;
import n.ClusterReplicateRetainedMessage;
import n.ClusterReplicateSubscriptions;
import n.ClusterReplicateTopicTree;
import o.ClusterTcpTransport;
import o.ClusterUdpTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.stream.Collectors;

public class ConfigurationReader {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationReader.class);
    private final ConfigFile configFile;
    private final GeneralConfigurationService generalConfigurationService;
    private final MqttConfigurationService mqttConfigurationService;
    private final ThrottlingConfigurationService throttlingConfigurationService;
    private final ListenerConfigurationService listenerConfigurationService;
    private final InternalConfigurationService internalConfigurationService;
    private final PersistenceConfigurationService persistenceConfigurationService;
    private final RestConfigurationService restConfigurationService;
    private final SharedSubscriptionsConfigurationService sharedSubscriptionsConfigurationService;
    private final ClusterConfigurationService clusterConfigurationService;
    private final EnvironmentVariableReplacer environmentVariableReplacer;

    @Inject
    public ConfigurationReader(ConfigFile configFile,
                               GeneralConfigurationService generalConfigurationService,
                               MqttConfigurationService mqttConfigurationService,
                               ThrottlingConfigurationService throttlingConfigurationService,
                               ListenerConfigurationService listenerConfigurationService,
                               InternalConfigurationService internalConfigurationService,
                               PersistenceConfigurationService persistenceConfigurationService,
                               RestConfigurationService restConfigurationService,
                               SharedSubscriptionsConfigurationService sharedSubscriptionsConfigurationService,
                               ClusterConfigurationService clusterConfigurationService,
                               EnvironmentVariableReplacer environmentVariableReplacer) {
        this.configFile = configFile;
        this.generalConfigurationService = generalConfigurationService;
        this.mqttConfigurationService = mqttConfigurationService;
        this.throttlingConfigurationService = throttlingConfigurationService;
        this.listenerConfigurationService = listenerConfigurationService;
        this.internalConfigurationService = internalConfigurationService;
        this.persistenceConfigurationService = persistenceConfigurationService;
        this.restConfigurationService = restConfigurationService;
        this.sharedSubscriptionsConfigurationService = sharedSubscriptionsConfigurationService;
        this.clusterConfigurationService = clusterConfigurationService;
        this.environmentVariableReplacer = environmentVariableReplacer;
    }

    public void read() {
        HiveMQConfigEntity configEntity = new HiveMQConfigEntity();
        updateConfig(configEntity);
        readConfigFile();
    }

    private void readConfigFile() {
        if (!this.configFile.get().isPresent()) {
            return;
        }
        File file = this.configFile.get().get();
        LOGGER.debug("Reading configuration file {}", file);
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(HiveMQConfigEntity.class);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            String content = new String(Files.readAllBytes(file.toPath()));
            content = this.environmentVariableReplacer.replace(content);
            ByteArrayInputStream input = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
            HiveMQConfigEntity configEntity = (HiveMQConfigEntity) unmarshaller.unmarshal(input);
            updateConfig(configEntity);
        } catch (Exception e) {
            if (e.getCause() instanceof UnrecoverableException) {
                if (((UnrecoverableException) e.getCause()).isShowException()) {
                    LOGGER.error("An unrecoverable Exception occurred. Exiting HiveMQ", e);
                    LOGGER.debug("Original error message:", e);
                }
                System.exit(1);
            }
            LOGGER.error("Could not read the configuration file {}. Using default config", file.getAbsolutePath());
            LOGGER.debug("Original error message:", e);
        }
    }

    private void updateConfig(HiveMQConfigEntity configEntity) {
        updateGeneralConfiguration(configEntity);
        updateListenerConfiguration(configEntity);
        updateMqttConfiguration(configEntity);
        updateThrottlingConfiguration(configEntity);
        updateInternalConfiguration(configEntity);
        updateSharedSubscriptionConfiguration(configEntity);
        updatePersistenceConfiguration(configEntity);
        updateRestConfiguration(configEntity);
        updateClusterConfiguration(configEntity);
    }

    private void updateRestConfiguration(HiveMQConfigEntity configEntity) {
        this.restConfigurationService.setJaxRsPath(configEntity.getRestService().getJaxRsPath());
        this.restConfigurationService.setServletPath(configEntity.getRestService().getServletPath());
        configEntity.getRestService().getListeners().stream()
                .filter(restListenerEntity -> restListenerEntity instanceof HttpRestListenerEntity)
                .map(restListenerEntity -> new HttpListener(restListenerEntity.getName(),
                        restListenerEntity.getBindAddress(), restListenerEntity.getPort()))
                .forEach(this.restConfigurationService::addListener);
    }

    private void updateGeneralConfiguration(HiveMQConfigEntity configEntity) {
        this.generalConfigurationService.setUpdateCheckEnabled(
                configEntity.getGeneral().isUpdateCheckEnabled());
    }

    private void updateListenerConfiguration(HiveMQConfigEntity configEntity) {
        ImmutableList<Listener> listeners = getListeners(configEntity);
        listeners.forEach(this.listenerConfigurationService::addListener);
    }

    private ImmutableList<Listener> getListeners(HiveMQConfigEntity configEntity) {
        ImmutableList.Builder<Listener> listenerBuilder = ImmutableList.builder();
        List<ListenerEntity> listenerEntities = configEntity.getListeners();
        listenerBuilder.addAll(
                listenerEntities.stream()
                        .filter(listenerEntity -> listenerEntity instanceof TCPListenerEntity)
                        .map(listenerEntity -> new TcpListener(listenerEntity.getPort(), listenerEntity.getBindAddress()))
                        .collect(Collectors.toList())
        );
        listenerBuilder.addAll(
                listenerEntities.stream()
                        .filter(listenerEntity -> listenerEntity instanceof WebsocketListenerEntity)
                        .map(listenerEntity -> (WebsocketListenerEntity) listenerEntity)
                        .map(listenerEntity ->
                                new WebsocketListener.Builder()
                                        .allowExtensions(listenerEntity.isAllowExtensions())
                                        .bindAddress(listenerEntity.getBindAddress())
                                        .path(listenerEntity.getPath())
                                        .port(listenerEntity.getPort())
                                        .setSubprotocols(listenerEntity.getSubprotocols())
                                        .build()
                        )
                        .collect(Collectors.toList())
        );
        listenerBuilder.addAll(
                listenerEntities.stream()
                        .filter(listenerEntity -> listenerEntity instanceof TlsTCPListenerEntity)
                        .map(listenerEntity -> (TlsTCPListenerEntity) listenerEntity)
                        .map(listenerEntity ->
                                new TlsTcpListener(listenerEntity.getPort(),
                                        listenerEntity.getBindAddress(),
                                        getTls(listenerEntity.getTls()))
                        )
                        .collect(Collectors.toList())
        );

        listenerBuilder.addAll(
                listenerEntities.stream()
                        .filter(listenerEntity -> listenerEntity instanceof TlsWebsocketListenerEntity)
                        .map(listenerEntity -> (TlsWebsocketListenerEntity) listenerEntity)
                        .map(listenerEntity ->
                                new TlsWebsocketListener.Builder()
                                        .port(listenerEntity.getPort())
                                        .bindAddress(listenerEntity.getBindAddress())
                                        .path(listenerEntity.getPath())
                                        .allowExtensions(listenerEntity.isAllowExtensions())
                                        .tls(getTls(listenerEntity.getTls()))
                                        .setSubprotocols(listenerEntity.getSubprotocols())
                                        .build()
                        )
                        .collect(Collectors.toList())
        );
        return listenerBuilder.build();
    }

    private Tls getTls(TLSEntity tlsEntity) {
        ClientAuthenticationModeEntity clientAuthenticationModeEntity = tlsEntity.getClientAuthMode();
        Tls.ClientAuthMode clientAuthMode;
        if (clientAuthenticationModeEntity == ClientAuthenticationModeEntity.OPTIONAL) {
            clientAuthMode = Tls.ClientAuthMode.OPTIONAL;
        } else if (clientAuthenticationModeEntity == ClientAuthenticationModeEntity.REQUIRED) {
            clientAuthMode = Tls.ClientAuthMode.REQUIRED;
        } else if (clientAuthenticationModeEntity == ClientAuthenticationModeEntity.NONE) {
            clientAuthMode = Tls.ClientAuthMode.NONE;
        } else {
            clientAuthMode = Tls.ClientAuthMode.NONE;
        }
        return new Tls(tlsEntity.getKeystore().getPath(),
                tlsEntity.getKeystore().getPassword(),
                "JKS",
                tlsEntity.getKeystore().getPrivateKeyPassword(),
                tlsEntity.getTruststore().getPath(),
                tlsEntity.getTruststore().getPassword(),
                "JKS",
                tlsEntity.getHandshakeTimeout(),
                clientAuthMode,
                tlsEntity.getProtocols(),
                tlsEntity.getCipherSuites());
    }

    private void updateMqttConfiguration(HiveMQConfigEntity configEntity) {
        MqttConfigEntity mqttEntity = configEntity.getMqtt();
        this.mqttConfigurationService.setRetryInterval(mqttEntity.getRetryInterval());
        this.mqttConfigurationService.setMaxClientIdLength(mqttEntity.getMaxClientIdLength());
        this.mqttConfigurationService.setNoConnectIdleTimeoutMillis(mqttEntity.getNoConnectIdleTimeoutMillis());
        this.mqttConfigurationService.setMaxQueuedMessages(configEntity.getMqtt().getMaxQueuedMessages().longValue());
    }

    private void updateThrottlingConfiguration(HiveMQConfigEntity configEntity) {
        ThrottlingEntity throttlingEntity = configEntity.getThrottling();
        this.throttlingConfigurationService.setMaxConnections(throttlingEntity.getMaxConnections());
        this.throttlingConfigurationService.setMaxMessageSize(throttlingEntity.getMaxMessageSize());
        this.throttlingConfigurationService.setIncomingLimitLock(throttlingEntity.getIncomingLimit());
        this.throttlingConfigurationService.setOutgoingLimit(throttlingEntity.getOutgoingLimit());
    }

    private void updateSharedSubscriptionConfiguration(HiveMQConfigEntity configEntity) {
        SharedSubscriptionsEntity subscriptionsEntity = configEntity.getSharedSubscriptions();
        if (subscriptionsEntity.getSharedSubscriptions() != null) {
            this.sharedSubscriptionsConfigurationService.setSharedSubscriptions(subscriptionsEntity.getSharedSubscriptions());
        }
    }

    private void updateInternalConfiguration(HiveMQConfigEntity configEntity) {
        InternalConfigEntity internalConfigEntity = configEntity.getInternal();
        internalConfigEntity.getOptions().forEach(optionEntity ->
                this.internalConfigurationService.set(optionEntity.getKey(), optionEntity.getValue())
        );
    }

    private void updatePersistenceConfiguration(HiveMQConfigEntity configEntity) {
        PersistenceConfigEntity persistenceEntity = configEntity.getPersistence();

        PersistenceModeEntity persistenceMode = persistenceEntity.getRetainedMessages().getPersistenceMode();
        this.persistenceConfigurationService.setRetainedMessagesMode(getPersistenceMode(persistenceMode));
        this.persistenceConfigurationService.setRetainedMessagesConfig(getPersistenceConfig(persistenceEntity.getRetainedMessages().getConfiguration()));

        ClientSessionPersistenceConfigEntity.ClientSessionGeneralConfigEntity clientSessionGeneralConfigEntity = persistenceEntity.getClientSessions().getGeneral();
        this.persistenceConfigurationService.setClientSessionGeneralMode(getPersistenceMode(clientSessionGeneralConfigEntity.getPersistenceMode()));
        this.persistenceConfigurationService.setClientSessionGeneralConfig(getPersistenceConfig(persistenceEntity.getClientSessions().getGeneral().getConfiguration()));

        ClientSessionPersistenceConfigEntity.ClientSessionSubscriptionsConfigEntity clientSessionSubscriptionsConfigEntity = persistenceEntity.getClientSessions().getSubscriptions();
        this.persistenceConfigurationService.setClientSessionSubscriptionMode(getPersistenceMode(clientSessionSubscriptionsConfigEntity.getPersistenceMode()));
        this.persistenceConfigurationService.setClientSessionSubscriptionConfig(getPersistenceConfig(persistenceEntity.getClientSessions().getSubscriptions().getConfiguration()));

        ClientSessionPersistenceConfigEntity.ClientSessionQueuedMessagesConfigEntity clientSessionQueuedMessagesConfigEntity = persistenceEntity.getClientSessions().getQueuedMessages();
        this.persistenceConfigurationService.setClientSessionQueuedMessagesMode(getPersistenceMode(clientSessionQueuedMessagesConfigEntity.getPersistenceMode()));
        this.persistenceConfigurationService.setMaxQueuedMessages(clientSessionQueuedMessagesConfigEntity.getMaxQueuedMessages());
        this.persistenceConfigurationService.setQueuedMessagesStrategy(getQueuedMessagesStrategy(clientSessionQueuedMessagesConfigEntity.getQueuedMessagesStrategy()));
        this.persistenceConfigurationService.setClientSessionQueuedMessagesConfig(getPersistenceConfig(persistenceEntity.getClientSessions().getQueuedMessages().getConfiguration()));

        MessageFlowPersistenceConfigEntity.MessageFlowIncomingConfigEntity messageFlowIncomingConfigEntity = persistenceEntity.getMessageFlow().getIncoming();
        this.persistenceConfigurationService.setMessageFlowIncomingMode(getPersistenceMode(messageFlowIncomingConfigEntity.getPersistenceMode()));
        this.persistenceConfigurationService.setMessageFlowIncomingConfig(getPersistenceConfig(persistenceEntity.getMessageFlow().getIncoming().getConfiguration()));

        MessageFlowPersistenceConfigEntity.MessageFlowOutgoingConfigEntity messageFlowOutgoingConfigEntity = persistenceEntity.getMessageFlow().getOutgoing();
        this.persistenceConfigurationService.setMessageFlowOutgoingMode(getPersistenceMode(messageFlowOutgoingConfigEntity.getPersistenceMode()));
        this.persistenceConfigurationService.setMessageFlowOutgoingConfig(getPersistenceConfig(persistenceEntity.getMessageFlow().getOutgoing().getConfiguration()));
    }

    private void updateClusterConfiguration(HiveMQConfigEntity configEntity) {
        this.clusterConfigurationService.setEnabled(configEntity.getCluster().isEnabled());
        ClusterTransport clusterTransport = getClusterTransport(configEntity);
        this.clusterConfigurationService.setTransport(clusterTransport);
        ClusterDiscovery clusterDiscovery = getClusterDiscovery(configEntity, clusterTransport == null ? ClusterTransport.Type.UDP : clusterTransport.getType());
        this.clusterConfigurationService.setDiscovery(clusterDiscovery);
        this.clusterConfigurationService.setReplicates(getClusterReplicates(configEntity));
        if (this.clusterConfigurationService.isEnabled()) {
            this.clusterConfigurationService.setFailureDetection(getFailureDetection(configEntity));
        }
    }


    @Nullable
    private ClusterTransport getClusterTransport(HiveMQConfigEntity configEntity) {
        List<ClusterTransportEntity> transportEntities = configEntity.getCluster().getTransport();
        ClusterTransportEntity clusterTransportEntity;
        if (transportEntities.size() < 1) {
            clusterTransportEntity = new ClusterUdpTransportEntity();
        } else if (transportEntities.size() > 1) {
            LOGGER.error("HiveMQ cluster can only use one kind of transport, please check your configuration.");
            clusterTransportEntity = transportEntities.get(0);
        } else {
            clusterTransportEntity = transportEntities.get(0);
        }
        ClusterTransport clusterTransport = null;
        if (clusterTransportEntity instanceof ClusterUdpTransportEntity) {
            ClusterUdpTransport clusterUdpTransport = new ClusterUdpTransport(clusterTransportEntity.getBindPort(), clusterTransportEntity.getBindAddress());
            clusterUdpTransport.setMulticastAddress(((ClusterUdpTransportEntity) clusterTransportEntity).getMulticastAddress());
            clusterUdpTransport.setMulticastEnabled(((ClusterUdpTransportEntity) clusterTransportEntity).isMulticastEnabled().booleanValue());
            clusterUdpTransport.setMulticastPort(((ClusterUdpTransportEntity) clusterTransportEntity).getMulticastPort());
            clusterTransport = clusterUdpTransport;
        } else if (clusterTransportEntity instanceof ClusterTcpTransportEntity) {
            ClusterTcpTransport clusterTcpTransport = new ClusterTcpTransport(clusterTransportEntity.getBindPort(), clusterTransportEntity.getBindAddress());
            clusterTcpTransport.setClientBindAddress(((ClusterTcpTransportEntity) clusterTransportEntity).getClientBindAddress());
            clusterTcpTransport.setClientBindPort(((ClusterTcpTransportEntity) clusterTransportEntity).getClientBindPort());
            clusterTransport = clusterTcpTransport;
        }
        return clusterTransport;
    }

    @Nullable
    private ClusterDiscovery getClusterDiscovery(HiveMQConfigEntity configEntity, ClusterTransport.Type transportType) {
        List<ClusterDiscoveryEntity> discoveryEntities = configEntity.getCluster().getDiscovery();
        ClusterDiscoveryEntity discoveryEntity;
        if (discoveryEntities.size() < 1) {
            if (transportType == ClusterTransport.Type.UDP) {
                discoveryEntity = new ClusterMulticastDiscoveryEntity();
            } else {
                discoveryEntity = new ClusterBroadcastDiscoveryEntity();
            }
        } else if (discoveryEntities.size() > 1) {
            LOGGER.error("HiveMQ cluster can only use one kind of discovery, please check your configuration.");
            discoveryEntity = discoveryEntities.get(0);
        } else {
            discoveryEntity = discoveryEntities.get(0);
        }
        ClusterDiscovery discovery = null;
        if (discoveryEntity instanceof ClusterBroadcastDiscoveryEntity) {
            ClusterBroadcastDiscoveryEntity entity = (ClusterBroadcastDiscoveryEntity) discoveryEntity;
            BroadcastDiscovery broadcastDiscovery = new BroadcastDiscovery();
            broadcastDiscovery.setPort(entity.getPort());
            broadcastDiscovery.setBroadcastAddress(entity.getBroadcastAddress());
            broadcastDiscovery.setPortRange(entity.getPortRange());
            discovery = broadcastDiscovery;
        } else if ((discoveryEntity instanceof ClusterMulticastDiscoveryEntity)) {
            discovery = new MulticastDiscovery();
        } else if ((discoveryEntity instanceof ClusterPluginDiscoveryEntity)) {
            PluginDiscovery pluginDiscovery = new PluginDiscovery();
            pluginDiscovery.setInterval(((ClusterPluginDiscoveryEntity) discoveryEntity).getInterval());
            discovery = pluginDiscovery;
        } else if ((discoveryEntity instanceof ClusterStaticDiscoveryEntity)) {
            ClusterStaticDiscoveryEntity entity = (ClusterStaticDiscoveryEntity) discoveryEntity;
            StaticDiscovery staticDiscovery = new StaticDiscovery();
            List<DiscoveryNode> nodes = entity.getNodes().stream()
                    .map(nodeEntity -> new DiscoveryNode(nodeEntity.getHost(), nodeEntity.getPort()))
                    .collect(Collectors.toList());
            staticDiscovery.setNodes(nodes);
            discovery = staticDiscovery;
        }
        return discovery;
    }

    private ClusterReplicates getClusterReplicates(HiveMQConfigEntity configEntity) {
        ClusterReplicatesEntity entity = configEntity.getCluster().getReplicates();
        ClusterReplicateClientSession clientSession = new ClusterReplicateClientSession();
        clientSession.setReplicateCount(entity.getClientSessions().getReplicateCount());

        ClusterReplicateQueuedMessages queuedMessages = new ClusterReplicateQueuedMessages();
        queuedMessages.setReplicateCount(entity.getQueuedMessages().getReplicateCount());

        ClusterReplicateOutgoingMessageFlow outgoingMessageFlow = new ClusterReplicateOutgoingMessageFlow();
        outgoingMessageFlow.setReplicateCount(entity.getOutgoingMessageFlow().getReplicateCount());
        outgoingMessageFlow.setReplicationInterval(entity.getOutgoingMessageFlow().getReplicationInterval());

        ClusterReplicateRetainedMessage retainedMessage = new ClusterReplicateRetainedMessage();
        retainedMessage.setReplicateCount(entity.getRetainedMessages().getReplicateCount());

        ClusterReplicateSubscriptions subscriptions = new ClusterReplicateSubscriptions();
        subscriptions.setReplicateCount(entity.getSubscriptions().getReplicateCount());

        ClusterReplicateTopicTree topicTree = new ClusterReplicateTopicTree();
        topicTree.setReplicateCount(entity.getTopicTree().getReplicateCount());

        return new ClusterReplicates(clientSession, queuedMessages, outgoingMessageFlow, retainedMessage, subscriptions, topicTree);
    }

    private ClusterFailureDetection getFailureDetection(HiveMQConfigEntity configEntity) {
        ClusterFailureDetectionEntity entity = configEntity.getCluster().getFailureDetection();
        TcpHealthCheckConfig tcpHealthCheck = new TcpHealthCheckConfig();
        tcpHealthCheck.setEnabled(entity.getHealthCheck().getEnabled().booleanValue());
        tcpHealthCheck.setBindAddress(entity.getHealthCheck().getBindAddress());
        tcpHealthCheck.setBindPort(entity.getHealthCheck().getBindPort());
        tcpHealthCheck.setPortRange(entity.getHealthCheck().getPortRange());
        HeartbeatConfig heartbeat = new HeartbeatConfig();
        heartbeat.setEnabled(entity.getHeartbeat().getEnabled().booleanValue());
        Integer interval = entity.getHeartbeat().getInterval();
        ClusterTransport.Type transportType = this.clusterConfigurationService.getTransport().getType();
        if (interval == null) {
            switch (transportType) {
                case TCP:
                    interval = ClusterHeartbeatEntity.INTERVAL_TCP_DEFAULT;
                    break;
                case UDP:
                    interval = ClusterHeartbeatEntity.INTERVAL_UDP_DEFAULT;
            }
        }
        heartbeat.setInterval(interval);
        Integer timeout = entity.getHeartbeat().getTimeout();
        if (timeout == null) {
            switch (transportType) {
                case TCP:
                    timeout = ClusterHeartbeatEntity.TIMEOUT_TCP_DEFAULT;
                    break;
                case UDP:
                    timeout = ClusterHeartbeatEntity.TIMEOUT_UDP_DEFAULT;
            }
        }
        heartbeat.setTimeout(timeout);
        return new ClusterFailureDetection(tcpHealthCheck, heartbeat);
    }


    private PersistenceMode getPersistenceMode(PersistenceModeEntity entity) {
        switch (entity) {
            case IN_MEMORY:
                return PersistenceMode.IN_MEMORY;
            case FILE:
                return PersistenceMode.FILE;
        }
        return PersistenceMode.FILE;
    }

    private QueuedMessagesStrategy getQueuedMessagesStrategy(ClientSessionPersistenceConfigEntity.ClientSessionQueuedMessagesConfigEntity.QueuedMessagesStrategy strategy) {
        switch (strategy) {
            case DISCARD_OLDEST:
                return QueuedMessagesStrategy.DISCARD_OLDEST;
            case DISCARD:
                return QueuedMessagesStrategy.DISCARD;
        }
        return QueuedMessagesStrategy.DISCARD;
    }

    private PersistenceConfig getPersistenceConfig(PersistenceConfigConfigurationEntity entity) {
        GCTypeEntity gcTypeEntity = entity.getGcTypeEntity();
        if (gcTypeEntity == null) {
            gcTypeEntity = GCTypeEntity.DELETE;
        }
        GCType gcType;
        switch (gcTypeEntity) {
            case DELETE:
                gcType = GCType.DELETE;
                break;
            case RENAME:
                gcType = GCType.RENAME;
                break;
            default:
                gcType = GCType.RENAME;
        }
        return new PersistenceConfigImpl(
                entity.isJmxEnabled(),
                gcType,
                entity.getGcDeletionDelay(),
                entity.getGcRunPeriod(),
                entity.getGcFilesInterval(),
                entity.getGcMinAge(),
                entity.getSyncPeriod(),
                entity.isDurableWrites());
    }
}
