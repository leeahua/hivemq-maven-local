package bo;

import av.InternalConfigurationService;
import av.Internals;
import bu.InternalPublish;
import cb1.AttributeKeys;
import cb1.ChannelUtils;
import cb1.WildcardValidationUtils;
import ck.PluginOnPublish;
import co.PublishServiceImpl;
import com.google.common.eventbus.EventBus;
import com.hivemq.spi.callback.events.OnPublishReceivedCallback;
import com.hivemq.spi.callback.registry.CallbackRegistry;
import com.hivemq.spi.callback.security.OnAuthorizationCallback;
import com.hivemq.spi.message.QoS;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

public class MqttPublishHandler extends SimpleChannelInboundHandler<InternalPublish> {
    private static final Logger LOGGER = LoggerFactory.getLogger(MqttPublishHandler.class);
    private final CallbackRegistry callbackRegistry;
    private final EventBus eventBus;
    private final PublishQueue publishQueue;
    private final PublishServiceImpl publishService;
    private final boolean messageOrderingQoSZero;

    @Inject
    MqttPublishHandler(CallbackRegistry callbackRegistry,
                       PublishServiceImpl publishService,
                       EventBus eventBus,
                       PublishQueue publishQueue,
                       InternalConfigurationService internalConfigurationService) {
        this.callbackRegistry = callbackRegistry;
        this.eventBus = eventBus;
        this.publishQueue = publishQueue;
        this.publishService = publishService;
        this.messageOrderingQoSZero = internalConfigurationService.getBoolean(Internals.MESSAGE_ORDERING_QOS_ZERO);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, InternalPublish msg) throws Exception {
        if (!WildcardValidationUtils.validPublishTopic(msg.getTopic())) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Client {} published to an invalid topic ( '{}' ). Disconnecting client.",
                        ChannelUtils.remoteIP(ctx.channel()).orElse("UNKNWON"), msg.getTopic());
            }
            ctx.close();
            return;
        }
        if (!exceedMaxPublishMessageSize(ctx, msg)) {
            ctx.close();
            return;
        }
        publish(ctx, msg);
    }

    private void publish(ChannelHandlerContext ctx, InternalPublish publish) {
        if (!this.callbackRegistry.isAvailable(OnAuthorizationCallback.class) &&
                !this.callbackRegistry.isAvailable(OnPublishReceivedCallback.class)) {
            this.publishService.publish(publish, ctx.channel().eventLoop());
            return;
        }
        if (this.messageOrderingQoSZero ||
                publish.getQoS() != QoS.AT_MOST_ONCE) {
            this.publishQueue.enqueue(ctx.channel().attr(AttributeKeys.MQTT_CLIENT_ID).get(), publish);
        }
        this.eventBus.post(new PluginOnPublish(ctx, publish, ChannelUtils.clientToken(ctx.channel())));
    }

    private boolean exceedMaxPublishMessageSize(ChannelHandlerContext ctx, InternalPublish publish) {
        Long restrictionMaxPublishMessageSize = ctx.channel().attr(AttributeKeys.RESTRICTION_MAX_PUBLISH_MESSAGE_SIZE).get();
        if (restrictionMaxPublishMessageSize != null &&
                restrictionMaxPublishMessageSize < publish.getPayload().length) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Client {} published a message with {} bytes payload its max allowed size is {} bytes. Disconnecting client.",
                        ChannelUtils.remoteIP(ctx.channel()).orElse("UNKNWON"), publish.getPayload().length, restrictionMaxPublishMessageSize);
            }
            return false;
        }
        return true;
    }
}
