package e;

import ae1.LicenseInformation;
import ah1.LicenseConnectionLimiter;
import al.ConnAckMessageDecoder;
import al.ConnectMessageDecoder;
import al.DisconnectMessageDecoder;
import al.PingReqMessageDecoder;
import al.PubAckMessageDecoder;
import al.PubCompMessageDecoder;
import al.PubRecMessageDecoder;
import al.PubRelMessageDecoder;
import al.PublishMessageDecoder;
import al.SubAckMessageDecoder;
import al.SubscribeMessageDecoder;
import al.UnsubAckMessageDecoder;
import al.UnsubscribeMessageDecoder;
import am1.Metrics;
import ap1.StatisticsInitializer;
import av.HiveMQConfigurationService;
import bj.MqttConnAckHandler;
import bj.MqttConnectHandler;
import bj.MqttConnectPersistenceHandler;
import bk.MqttDisconnectHandler;
import bl.MqttLwtHandler;
import bm.MqttOrderedTopicHandler;
import bn.MqttPingReqHandler;
import bo.MqttMessageIdReturnHandler;
import bo.MqttPublishHandler;
import bo.MqttPublishUserEventHandler;
import bp.MqttQosReceiverHandler;
import bp.MqttQosSenderHandler;
import bq.MqttSubscribeHandler;
import bs.MqttUnsubscribeHandler;
import cg.PluginOnConnAckSendCallbackHandler;
import cg.PluginOnDisconnectCallbackHandler;
import cg.PluginOnPingCallbackHandler;
import cg.PluginOnPubAckReceivedCallbackHandler;
import cg.PluginOnPubAckSendCallbackHandler;
import cg.PluginOnPubCompReceivedCallbackHandler;
import cg.PluginOnPubCompSendCallbackHandler;
import cg.PluginOnPubRecReceivedCallbackHandler;
import cg.PluginOnPubRecSendCallbackHandler;
import cg.PluginOnPubRelReceivedCallbackHandler;
import cg.PluginOnPubRelSendCallbackHandler;
import cg.PluginOnPublishSendCallbackHandler;
import cg.PluginOnSubAckSendCallbackHandler;
import cg.PluginOnSubscribeCallbackHandler;
import cg.PluginOnUnsubAckSendCallbackHandler;
import cg.PluginOnUnsubscribeCallbackHandler;
import ci.PluginOnAuthorizationCallbackHandler;
import ci.PluginOnInsufficientPermissionDisconnectCallbackHandler;
import com.hivemq.spi.services.configuration.ThrottlingConfigurationService;
import io.netty.channel.group.ChannelGroup;
import io.netty.handler.traffic.GlobalTrafficShapingHandler;

import javax.inject.Inject;
import javax.inject.Provider;

public class ChannelPipelineDependencies {
    private final Provider<StatisticsInitializer> statisticsInitializerProvider;
    private final Provider<MqttConnectHandler> mqttConnectHandlerProvider;
    private final MqttConnectPersistenceHandler mqttConnectPersistenceHandler;
    private final MqttLwtHandler mqttLwtHandler;
    private final MqttDisconnectHandler mqttDisconnectHandler;
    private final Provider<MqttSubscribeHandler> mqttSubscribeHandlerProvider;
    private final Provider<MqttPublishHandler> mqttPublishHandlerProvider;
    private final Provider<MqttPublishUserEventHandler> mqttPublishUserEventHandlerProvider;
    private final Provider<MqttUnsubscribeHandler> mqttUnsubscribeHandlerProvider;
    private final Provider<MqttQosReceiverHandler> mqttQosReceiverHandlerProvider;
    private final Provider<MqttQosSenderHandler> mqttQosSenderHandlerProvider;
    private final Provider<MqttOrderedTopicHandler> mqttOrderedTopicHandlerProvider;
    private final MqttConnAckHandler mqttConnAckHandler;
    private final ChannelGroup channelGroup;
    private final PluginOnDisconnectCallbackHandler pluginOnDisconnectCallbackHandler;
    private final PluginOnPingCallbackHandler pluginOnPingCallbackHandler;
    private final PluginOnPublishSendCallbackHandler pluginOnPublishSendCallbackHandler;
    private final PluginOnPubAckSendCallbackHandler pluginOnPubAckSendCallbackHandler;
    private final PluginOnPubAckReceivedCallbackHandler pluginOnPubAckReceivedCallbackHandler;
    private final PluginOnPubCompReceivedCallbackHandler pluginOnPubCompReceivedCallbackHandler;
    private final PluginOnUnsubscribeCallbackHandler pluginOnUnsubscribeCallbackHandler;
    private final PluginOnSubscribeCallbackHandler pluginOnSubscribeCallbackHandler;
    private final PluginOnConnAckSendCallbackHandler pluginOnConnAckSendCallbackHandler;
    private final LicenseConnectionLimiter licenseConnectionLimiter;
    private final Provider<LicenseInformation> y;
    private final HiveMQConfigurationService hiveMQConfigurationService;
    private final GlobalTrafficShapingHandler globalTrafficShapingHandler;
    private final PluginOnAuthorizationCallbackHandler getPluginOnAuthorizationCallbackHandler;
    private final PluginOnInsufficientPermissionDisconnectCallbackHandler pluginOnInsufficientPermissionDisconnectCallbackHandler;
    private final Metrics metrics;
    private final ExceptionHandler exceptionHandler;
    private final MqttPingReqHandler mqttPingReqHandler;
    private final PluginOnPubRecReceivedCallbackHandler pluginOnPubRecReceivedCallbackHandler;
    private final PluginOnPubRelReceivedCallbackHandler pluginOnPubRelReceivedCallbackHandler;
    private final PluginOnPubCompSendCallbackHandler pluginOnPubCompSendCallbackHandler;
    private final PluginOnPubRecSendCallbackHandler pluginOnPubRecSendCallbackHandler;
    private final PluginOnPubRelSendCallbackHandler pluginOnPubRelSendCallbackHandler;
    private final PluginOnSubAckSendCallbackHandler pluginOnSubAckSendCallbackHandler;
    private final PluginOnUnsubAckSendCallbackHandler pluginOnUnsubAckSendCallbackHandler;
    private final ThrottlingConfigurationService throttlingConfigurationService;
    private final ConnectMessageDecoder connectMessageDecoder;
    private final ConnAckMessageDecoder connAckMessageDecoder;
    private final PingReqMessageDecoder pingReqMessageDecoder;
    private final PublishMessageDecoder publishMessageDecoder;
    private final PubAckMessageDecoder pubAckMessageDecoder;
    private final PubRecMessageDecoder pubRecMessageDecoder;
    private final PubCompMessageDecoder pubCompMessageDecoder;
    private final PubRelMessageDecoder pubRelMessageDecoder;
    private final DisconnectMessageDecoder disconnectMessageDecoder;
    private final SubscribeMessageDecoder subscribeMessageDecoder;
    private final UnsubscribeMessageDecoder unsubscribeMessageDecoder;
    private final SubAckMessageDecoder subAckMessageDecoder;
    private final UnsubAckMessageDecoder unsubAckMessageDecoder;
    private final MqttMessageIdReturnHandler mqttMessageIdReturnHandler;

    @Inject
    public ChannelPipelineDependencies(Provider<StatisticsInitializer> statisticsInitializerProvider,
                                       Provider<MqttConnectHandler> mqttConnectHandlerProvider,
                                       MqttConnectPersistenceHandler mqttConnectPersistenceHandler,
                                       MqttLwtHandler mqttLwtHandler,
                                       MqttDisconnectHandler mqttDisconnectHandler,
                                       Provider<MqttSubscribeHandler> mqttSubscribeHandlerProvider,
                                       Provider<MqttPublishHandler> mqttPublishHandlerProvider,
                                       Provider<MqttPublishUserEventHandler> mqttPublishUserEventHandlerProvider,
                                       Provider<MqttUnsubscribeHandler> mqttUnsubscribeHandlerProvider,
                                       Provider<MqttQosReceiverHandler> mqttQosReceiverHandlerProvider,
                                       Provider<MqttQosSenderHandler> mqttQosSenderHandlerProvider,
                                       Provider<MqttOrderedTopicHandler> mqttOrderedTopicHandlerProvider,
                                       MqttConnAckHandler mqttConnAckHandler,
                                       ChannelGroup channelGroup,
                                       PluginOnDisconnectCallbackHandler pluginOnDisconnectCallbackHandler,
                                       PluginOnPingCallbackHandler pluginOnPingCallbackHandler,
                                       PluginOnPublishSendCallbackHandler pluginOnPublishSendCallbackHandler,
                                       PluginOnPubAckSendCallbackHandler pluginOnPubAckSendCallbackHandler,
                                       PluginOnPubAckReceivedCallbackHandler pluginOnPubAckReceivedCallbackHandler,
                                       PluginOnPubCompReceivedCallbackHandler pluginOnPubCompReceivedCallbackHandler,
                                       PluginOnUnsubscribeCallbackHandler pluginOnUnsubscribeCallbackHandler,
                                       PluginOnSubscribeCallbackHandler pluginOnSubscribeCallbackHandler,
                                       PluginOnConnAckSendCallbackHandler pluginOnConnAckSendCallbackHandler,
                                       LicenseConnectionLimiter licenseConnectionLimiter,
                                       Provider<LicenseInformation> paramProvider9,
                                       HiveMQConfigurationService hiveMQConfigurationService,
                                       GlobalTrafficShapingHandler globalTrafficShapingHandler,
                                       PluginOnAuthorizationCallbackHandler getPluginOnAuthorizationCallbackHandler,
                                       PluginOnInsufficientPermissionDisconnectCallbackHandler getPluginOnInsufficientPermissionDisconnectCallbackHandler,
                                       Metrics metrics,
                                       ExceptionHandler exceptionHandler,
                                       MqttPingReqHandler mqttPingReqHandler,
                                       PluginOnPubRelReceivedCallbackHandler pluginOnPubRelReceivedCallbackHandler,
                                       PluginOnPubRecReceivedCallbackHandler PluginOnPubRecReceivedCallbackHandler,
                                       PluginOnPubCompSendCallbackHandler pluginOnPubCompSendCallbackHandler,
                                       PluginOnPubRecSendCallbackHandler pluginOnPubRecSendCallbackHandler,
                                       PluginOnPubRelSendCallbackHandler pluginOnPubRelSendCallbackHandler,
                                       PluginOnSubAckSendCallbackHandler pluginOnSubAckSendCallbackHandler,
                                       PluginOnUnsubAckSendCallbackHandler pluginOnUnsubAckSendCallbackHandler,
                                       ThrottlingConfigurationService throttlingConfigurationService,
                                       ConnectMessageDecoder connectMessageDecoder,
                                       ConnAckMessageDecoder connAckMessageDecoder,
                                       PingReqMessageDecoder pingReqMessageDecoder,
                                       PublishMessageDecoder publishMessageDecoder,
                                       PubAckMessageDecoder pubAckMessageDecoder,
                                       PubRecMessageDecoder pubRecMessageDecoder,
                                       PubCompMessageDecoder pubCompMessageDecoder,
                                       PubRelMessageDecoder pubRelMessageDecoder,
                                       DisconnectMessageDecoder disconnectMessageDecoder,
                                       SubscribeMessageDecoder subscribeMessageDecoder,
                                       UnsubscribeMessageDecoder unsubscribeMessageDecoder,
                                       SubAckMessageDecoder subAckMessageDecoder,
                                       UnsubAckMessageDecoder unsubAckMessageDecoder,
                                       MqttMessageIdReturnHandler mqttMessageIdReturnHandler) {
        this.statisticsInitializerProvider = statisticsInitializerProvider;
        this.mqttConnectHandlerProvider = mqttConnectHandlerProvider;
        this.mqttConnectPersistenceHandler = mqttConnectPersistenceHandler;
        this.mqttLwtHandler = mqttLwtHandler;
        this.mqttDisconnectHandler = mqttDisconnectHandler;
        this.mqttSubscribeHandlerProvider = mqttSubscribeHandlerProvider;
        this.mqttPublishHandlerProvider = mqttPublishHandlerProvider;
        this.mqttPublishUserEventHandlerProvider = mqttPublishUserEventHandlerProvider;
        this.mqttUnsubscribeHandlerProvider = mqttUnsubscribeHandlerProvider;
        this.mqttQosReceiverHandlerProvider = mqttQosReceiverHandlerProvider;
        this.mqttQosSenderHandlerProvider = mqttQosSenderHandlerProvider;
        this.mqttOrderedTopicHandlerProvider = mqttOrderedTopicHandlerProvider;
        this.mqttConnAckHandler = mqttConnAckHandler;
        this.channelGroup = channelGroup;
        this.pluginOnDisconnectCallbackHandler = pluginOnDisconnectCallbackHandler;
        this.pluginOnPingCallbackHandler = pluginOnPingCallbackHandler;
        this.pluginOnPublishSendCallbackHandler = pluginOnPublishSendCallbackHandler;
        this.pluginOnPubAckSendCallbackHandler = pluginOnPubAckSendCallbackHandler;
        this.pluginOnPubAckReceivedCallbackHandler = pluginOnPubAckReceivedCallbackHandler;
        this.pluginOnPubCompReceivedCallbackHandler = pluginOnPubCompReceivedCallbackHandler;
        this.pluginOnUnsubscribeCallbackHandler = pluginOnUnsubscribeCallbackHandler;
        this.pluginOnSubscribeCallbackHandler = pluginOnSubscribeCallbackHandler;
        this.pluginOnConnAckSendCallbackHandler = pluginOnConnAckSendCallbackHandler;
        this.licenseConnectionLimiter = licenseConnectionLimiter;
        this.y = paramProvider9;
        this.hiveMQConfigurationService = hiveMQConfigurationService;
        this.globalTrafficShapingHandler = globalTrafficShapingHandler;
        this.getPluginOnAuthorizationCallbackHandler = getPluginOnAuthorizationCallbackHandler;
        this.pluginOnInsufficientPermissionDisconnectCallbackHandler = getPluginOnInsufficientPermissionDisconnectCallbackHandler;
        this.metrics = metrics;
        this.exceptionHandler = exceptionHandler;
        this.mqttPingReqHandler = mqttPingReqHandler;
        this.pluginOnPubRelReceivedCallbackHandler = pluginOnPubRelReceivedCallbackHandler;
        this.pluginOnPubRecReceivedCallbackHandler = PluginOnPubRecReceivedCallbackHandler;
        this.pluginOnPubCompSendCallbackHandler = pluginOnPubCompSendCallbackHandler;
        this.pluginOnPubRecSendCallbackHandler = pluginOnPubRecSendCallbackHandler;
        this.pluginOnPubRelSendCallbackHandler = pluginOnPubRelSendCallbackHandler;
        this.pluginOnSubAckSendCallbackHandler = pluginOnSubAckSendCallbackHandler;
        this.pluginOnUnsubAckSendCallbackHandler = pluginOnUnsubAckSendCallbackHandler;
        this.throttlingConfigurationService = throttlingConfigurationService;
        this.connectMessageDecoder = connectMessageDecoder;
        this.connAckMessageDecoder = connAckMessageDecoder;
        this.pingReqMessageDecoder = pingReqMessageDecoder;
        this.publishMessageDecoder = publishMessageDecoder;
        this.pubAckMessageDecoder = pubAckMessageDecoder;
        this.pubRecMessageDecoder = pubRecMessageDecoder;
        this.pubCompMessageDecoder = pubCompMessageDecoder;
        this.pubRelMessageDecoder = pubRelMessageDecoder;
        this.disconnectMessageDecoder = disconnectMessageDecoder;
        this.subscribeMessageDecoder = subscribeMessageDecoder;
        this.unsubscribeMessageDecoder = unsubscribeMessageDecoder;
        this.subAckMessageDecoder = subAckMessageDecoder;
        this.unsubAckMessageDecoder = unsubAckMessageDecoder;
        this.mqttMessageIdReturnHandler = mqttMessageIdReturnHandler;
    }

    public StatisticsInitializer getStatisticsInitializer() {
        return this.statisticsInitializerProvider.get();
    }

    public MqttConnectHandler getMqttConnectHandler() {
        return this.mqttConnectHandlerProvider.get();
    }

    public MqttLwtHandler getMqttLwtHandler() {
        return this.mqttLwtHandler;
    }

    public MqttDisconnectHandler getMqttDisconnectHandler() {
        return this.mqttDisconnectHandler;
    }

    public MqttSubscribeHandler getMqttSubscribeHandler() {
        return this.mqttSubscribeHandlerProvider.get();
    }

    public MqttPublishHandler getMqttPublishHandler() {
        return this.mqttPublishHandlerProvider.get();
    }

    public MqttPublishUserEventHandler getMqttPublishUserEventHandler() {
        return this.mqttPublishUserEventHandlerProvider.get();
    }

    public MqttUnsubscribeHandler getMqttUnsubscribeHandler() {
        return this.mqttUnsubscribeHandlerProvider.get();
    }

    public MqttQosSenderHandler getMqttQosSenderHandler() {
        return this.mqttQosSenderHandlerProvider.get();
    }

    public MqttQosReceiverHandler getMqttQosReceiverHandler() {
        return this.mqttQosReceiverHandlerProvider.get();
    }

    public ChannelGroup getChannelGroup() {
        return this.channelGroup;
    }

    public PluginOnDisconnectCallbackHandler getPluginOnDisconnectCallbackHandler() {
        return this.pluginOnDisconnectCallbackHandler;
    }

    public PluginOnSubscribeCallbackHandler getPluginOnSubscribeCallbackHandler() {
        return this.pluginOnSubscribeCallbackHandler;
    }

    public PluginOnPublishSendCallbackHandler getPluginOnPublishSendCallbackHandler() {
        return this.pluginOnPublishSendCallbackHandler;
    }

    public PluginOnUnsubscribeCallbackHandler getPluginOnUnsubscribeCallbackHandler() {
        return this.pluginOnUnsubscribeCallbackHandler;
    }

    public PluginOnPingCallbackHandler getPluginOnPingCallbackHandler() {
        return this.pluginOnPingCallbackHandler;
    }

    public LicenseConnectionLimiter getLicenseConnectionLimiter() {
        return this.licenseConnectionLimiter;
    }

    public Provider<LicenseInformation> r() {
        return this.y;
    }

    public HiveMQConfigurationService getHiveMQConfigurationService() {
        return this.hiveMQConfigurationService;
    }

    public GlobalTrafficShapingHandler getGlobalTrafficShapingHandler() {
        return this.globalTrafficShapingHandler;
    }

    public PluginOnAuthorizationCallbackHandler getPluginOnAuthorizationCallbackHandler() {
        return this.getPluginOnAuthorizationCallbackHandler;
    }

    public PluginOnInsufficientPermissionDisconnectCallbackHandler getPluginOnInsufficientPermissionDisconnectCallbackHandler() {
        return this.pluginOnInsufficientPermissionDisconnectCallbackHandler;
    }

    public Metrics getMetrics() {
        return this.metrics;
    }

    public ExceptionHandler getExceptionHandler() {
        return this.exceptionHandler;
    }

    public MqttPingReqHandler getMqttPingReqHandler() {
        return this.mqttPingReqHandler;
    }

    public MqttConnectPersistenceHandler getMqttConnectPersistenceHandler() {
        return this.mqttConnectPersistenceHandler;
    }

    public PluginOnConnAckSendCallbackHandler getPluginOnConnAckSendCallbackHandler() {
        return this.pluginOnConnAckSendCallbackHandler;
    }

    public MqttConnAckHandler getMqttConnAckHandler() {
        return this.mqttConnAckHandler;
    }

    public PluginOnPubAckSendCallbackHandler getPluginOnPubAckSendCallbackHandler() {
        return this.pluginOnPubAckSendCallbackHandler;
    }

    public PluginOnPubAckReceivedCallbackHandler getPluginOnPubAckReceivedCallbackHandler() {
        return this.pluginOnPubAckReceivedCallbackHandler;
    }

    public PluginOnPubRelReceivedCallbackHandler getPluginOnPubRelReceivedCallbackHandler() {
        return this.pluginOnPubRelReceivedCallbackHandler;
    }

    public PluginOnPubCompReceivedCallbackHandler getPluginOnPubCompReceivedCallbackHandler() {
        return this.pluginOnPubCompReceivedCallbackHandler;
    }

    public MqttOrderedTopicHandler getMqttOrderedTopicHandler() {
        return this.mqttOrderedTopicHandlerProvider.get();
    }

    public PluginOnPubRecReceivedCallbackHandler getPluginOnPubRecReceivedCallbackHandler() {
        return this.pluginOnPubRecReceivedCallbackHandler;
    }

    public PluginOnPubCompSendCallbackHandler getPluginOnPubCompSendCallbackHandler() {
        return this.pluginOnPubCompSendCallbackHandler;
    }

    public PluginOnPubRecSendCallbackHandler getPluginOnPubRecSendCallbackHandler() {
        return this.pluginOnPubRecSendCallbackHandler;
    }

    public PluginOnPubRelSendCallbackHandler getPluginOnPubRelSendCallbackHandler() {
        return this.pluginOnPubRelSendCallbackHandler;
    }

    public PluginOnSubAckSendCallbackHandler getPluginOnSubAckSendCallbackHandler() {
        return this.pluginOnSubAckSendCallbackHandler;
    }

    public PluginOnUnsubAckSendCallbackHandler getPluginOnUnsubAckSendCallbackHandler() {
        return this.pluginOnUnsubAckSendCallbackHandler;
    }

    public ThrottlingConfigurationService getThrottlingConfigurationService() {
        return this.throttlingConfigurationService;
    }

    public ConnectMessageDecoder getConnectMessageDecoder() {
        return this.connectMessageDecoder;
    }

    public ConnAckMessageDecoder getConnAckMessageDecoder() {
        return this.connAckMessageDecoder;
    }

    public PingReqMessageDecoder getPingReqMessageDecoder() {
        return this.pingReqMessageDecoder;
    }

    public PublishMessageDecoder getPublishMessageDecoder() {
        return this.publishMessageDecoder;
    }

    public PubAckMessageDecoder getPubAckMessageDecoder() {
        return this.pubAckMessageDecoder;
    }

    public PubRecMessageDecoder getPubRecMessageDecoder() {
        return this.pubRecMessageDecoder;
    }

    public PubCompMessageDecoder getPubCompMessageDecoder() {
        return this.pubCompMessageDecoder;
    }

    public PubRelMessageDecoder getPubRelMessageDecoder() {
        return this.pubRelMessageDecoder;
    }

    public DisconnectMessageDecoder getDisconnectMessageDecoder() {
        return this.disconnectMessageDecoder;
    }

    public SubscribeMessageDecoder getSubscribeMessageDecoder() {
        return this.subscribeMessageDecoder;
    }

    public UnsubscribeMessageDecoder getUnsubscribeMessageDecoder() {
        return this.unsubscribeMessageDecoder;
    }

    public SubAckMessageDecoder getSubAckMessageDecoder() {
        return this.subAckMessageDecoder;
    }

    public UnsubAckMessageDecoder getUnsubAckMessageDecoder() {
        return this.unsubAckMessageDecoder;
    }

    public MqttMessageIdReturnHandler getMqttMessageIdReturnHandler() {
        return this.mqttMessageIdReturnHandler;
    }
}
