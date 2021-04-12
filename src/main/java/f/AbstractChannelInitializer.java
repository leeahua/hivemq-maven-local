package f;

import al.MqttMessageDecoder;
import am.MqttMessageEncoder;
import bj.MqttMessageBarrier;
import bj.MqttSubscribeMessageBarrier;
import bj.NoConnectIdleEventHandler;
import cb1.ChannelUtils;
import com.hivemq.spi.services.configuration.MqttConfigurationService;
import com.hivemq.spi.util.SslException;
import e.ChannelPipelineDependencies;
import e.Pipelines;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.EventExecutorGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public abstract class AbstractChannelInitializer extends ChannelInitializer<Channel> {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractChannelInitializer.class);
    public static final String FIRST_HANDLER = Pipelines.ALL_CHANNEL_GROUP_HANDLER;
    private final ChannelPipelineDependencies dependencies;
    private final EventExecutorGroup eventExecutorGroup;

    public AbstractChannelInitializer(ChannelPipelineDependencies dependencies,
                                      EventExecutorGroup eventExecutorGroup) {
        this.dependencies = dependencies;
        this.eventExecutorGroup = eventExecutorGroup;
    }

    protected void initChannel(Channel channel) {
        addIdleHandler(channel);
        channel.pipeline().addLast(Pipelines.ALL_CHANNEL_GROUP_HANDLER, new AllChannelGroupHandler(this.dependencies.getChannelGroup()));
        channel.pipeline().addLast(Pipelines.GLOBAL_THROTTLING_HANDLER, this.dependencies.getGlobalTrafficShapingHandler());
        channel.pipeline().addLast(Pipelines.MQTT_MESSAGE_DECODER, new MqttMessageDecoder(this.dependencies));
        channel.pipeline().addLast(Pipelines.MQTT_MESSAGE_ENCODER, new MqttMessageEncoder(this.dependencies.getMetrics()));
        addLicenseConnectionLimiter(channel);
        channel.pipeline().addLast(Pipelines.MQTT_MESSAGE_BARRIER, new MqttMessageBarrier());
        channel.pipeline().addLast(Pipelines.MQTT_SUBSCRIBE_MESSAGE_BARRIER, new MqttSubscribeMessageBarrier());
        channel.pipeline().addLast(Pipelines.MQTT_MESSAGE_ID_RETURN_HANDLER, this.dependencies.getMqttMessageIdReturnHandler());
        channel.pipeline().addLast(Pipelines.MQTT_ORDERED_TOPIC_HANDLER, this.dependencies.getMqttOrderedTopicHandler());
        channel.pipeline().addLast(this.eventExecutorGroup, this.dependencies.getPluginOnPubAckSendCallbackHandler());
        channel.pipeline().addLast(this.eventExecutorGroup, this.dependencies.getPluginOnPubAckReceivedCallbackHandler());
        channel.pipeline().addLast(this.eventExecutorGroup, this.dependencies.getPluginOnPubRelReceivedCallbackHandler());
        channel.pipeline().addLast(this.eventExecutorGroup, this.dependencies.getPluginOnPubCompReceivedCallbackHandler());
        channel.pipeline().addLast(this.eventExecutorGroup, this.dependencies.getPluginOnPubRecReceivedCallbackHandler());
        channel.pipeline().addLast(this.eventExecutorGroup, this.dependencies.getPluginOnPubCompSendCallbackHandler());
        channel.pipeline().addLast(this.eventExecutorGroup, this.dependencies.getPluginOnPubRecSendCallbackHandler());
        channel.pipeline().addLast(this.eventExecutorGroup, this.dependencies.getPluginOnPubRelSendCallbackHandler());
        channel.pipeline().addLast(this.eventExecutorGroup, this.dependencies.getPluginOnSubAckSendCallbackHandler());
        channel.pipeline().addLast(this.eventExecutorGroup, this.dependencies.getPluginOnUnsubAckSendCallbackHandler());
        channel.pipeline().addLast(Pipelines.MQTT_CONNACK_HANDLER, this.dependencies.getMqttConnAckHandler());
        channel.pipeline().addLast(Pipelines.MQTT_CONNECT_HANDLER, this.dependencies.getMqttConnectHandler());
        channel.pipeline().addLast(this.eventExecutorGroup, Pipelines.MQTT_CONNECT_PERSISTENCE_HANDLER, this.dependencies.getMqttConnectPersistenceHandler());
        channel.pipeline().addLast(Pipelines.MQTT_LWT_HANDLER, this.dependencies.getMqttLwtHandler());
        channel.pipeline().addLast(Pipelines.MQTT_DISCONNECT_HANDLER, this.dependencies.getMqttDisconnectHandler());
        channel.pipeline().addLast(Pipelines.MQTT_SUBSCRIBE_HANDLER, this.dependencies.getMqttSubscribeHandler());
        channel.pipeline().addLast(Pipelines.MQTT_PINGREQ_HANDLER, this.dependencies.getMqttPingReqHandler());
        channel.pipeline().addLast(this.eventExecutorGroup, Pipelines.MQTT_QOS_RECEIVER_HANDLER, this.dependencies.getMqttQosReceiverHandler());
        channel.pipeline().addLast(this.eventExecutorGroup, Pipelines.MQTT_QOS_SENDER_HANDLER, this.dependencies.getMqttQosSenderHandler());
        channel.pipeline().addLast(Pipelines.MQTT_PUBLISH_USER_EVENT_HANDLER, this.dependencies.getMqttPublishUserEventHandler());
        channel.pipeline().addLast(Pipelines.MQTT_PUBLISH_HANDLER, this.dependencies.getMqttPublishHandler());
        channel.pipeline().addLast(Pipelines.MQTT_UNSUBSCRIBE_HANDLER, this.dependencies.getMqttUnsubscribeHandler());
        channel.pipeline().addLast(Pipelines.STATISTICS_INITIALIZER, this.dependencies.getStatisticsInitializer());
        channel.pipeline().addLast(this.eventExecutorGroup, this.dependencies.getPluginOnDisconnectCallbackHandler());
        channel.pipeline().addLast(this.eventExecutorGroup, this.dependencies.getPluginOnSubscribeCallbackHandler());
        channel.pipeline().addLast(this.eventExecutorGroup, this.dependencies.getPluginOnPublishSendCallbackHandler());
        channel.pipeline().addLast(this.eventExecutorGroup, this.dependencies.getPluginOnUnsubscribeCallbackHandler());
        channel.pipeline().addLast(this.eventExecutorGroup, this.dependencies.getPluginOnAuthorizationCallbackHandler());
        channel.pipeline().addLast(this.eventExecutorGroup, this.dependencies.getPluginOnInsufficientPermissionDisconnectCallbackHandler());
        channel.pipeline().addLast(this.eventExecutorGroup, this.dependencies.getPluginOnPingCallbackHandler());
        channel.pipeline().addLast(this.eventExecutorGroup, this.dependencies.getPluginOnConnAckSendCallbackHandler());
        initTransportChannel(channel);
        channel.pipeline().addLast(Pipelines.EXCEPTION_HANDLER, this.dependencies.getExceptionHandler());
    }

    protected void addIdleHandler(Channel channel) {
        MqttConfigurationService mqttConfigurationService = this.dependencies.getHiveMQConfigurationService().mqttConfiguration();
        long noConnectIdleTimeoutMillis = mqttConfigurationService.noConnectIdleTimeoutMillis();
        if (noConnectIdleTimeoutMillis > 0L) {
            IdleStateHandler newConnectionIdleHandler = new IdleStateHandler(noConnectIdleTimeoutMillis, 0L, 0L, TimeUnit.MILLISECONDS);
            channel.pipeline().addLast(Pipelines.NEW_CONNECTION_IDLE_HANDLER, newConnectionIdleHandler);
            channel.pipeline().addLast(Pipelines.NO_CONNECT_IDLE_EVENT_HANDLER, new NoConnectIdleEventHandler());
        }
    }

    private void addLicenseConnectionLimiter(Channel channel) {
        channel.pipeline().addLast(Pipelines.LICENSE_CONNECTION_LIMITER, this.dependencies.getLicenseConnectionLimiter());
    }

    protected abstract void initTransportChannel(Channel channel);

    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (cause instanceof SslException) {
            LOGGER.error("{}. Disconnecting client {} ",
                    cause.getMessage(), ChannelUtils.remoteIP(ctx.channel()).orElse("UNKNOWN"));
            LOGGER.debug("Original exception:", cause);
            ctx.close();
        } else {
            super.exceptionCaught(ctx, cause);
        }
    }
}
