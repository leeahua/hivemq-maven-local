package ck;

import av.InternalConfigurationService;
import av.Internals;
import bl1.ChannelPersistence;
import bo.PublishQueue;
import bs1.PluginOnAuthorizationPublish;
import bs1.PluginOnAuthorizationPublishCompleted;
import bs1.PluginOnInsufficientPermissionDisconnect;
import bs1.PluginOnInsufficientPermissionDisconnect.Type;
import bu.InternalPublish;
import bw1.PluginOnPublishReceivedCompleted;
import bw1.PluginOnPublishReceived;
import co.PublishServiceImpl;
import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.inject.Inject;
import com.hivemq.spi.callback.events.OnPublishReceivedCallback;
import com.hivemq.spi.callback.registry.CallbackRegistry;
import com.hivemq.spi.callback.security.OnAuthorizationCallback;
import com.hivemq.spi.callback.security.OnInsufficientPermissionDisconnect;
import com.hivemq.spi.callback.security.authorization.AuthorizationBehaviour;
import com.hivemq.spi.message.QoS;
import com.hivemq.spi.security.ClientData;
import d.CacheScoped;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import s.Cluster;

import java.util.concurrent.ExecutorService;

@CacheScoped
public class PublishHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(PublishHandler.class);
    private final EventBus eventBus;
    private final CallbackRegistry callbackRegistry;
    private final PublishServiceImpl publishService;
    private final PublishQueue publishQueue;
    private final boolean messageOrderingOoSZero;
    private final ChannelPersistence channelPersistence;
    private final ListeningExecutorService clusterExecutor;

    @Inject
    public PublishHandler(EventBus eventBus,
                             CallbackRegistry callbackRegistry,
                             PublishServiceImpl publishService,
                             PublishQueue publishQueue,
                             InternalConfigurationService internalConfigurationService,
                             ChannelPersistence channelPersistence,
                             @Cluster ListeningExecutorService clusterExecutor) {
        this.eventBus = eventBus;
        this.callbackRegistry = callbackRegistry;
        this.publishService = publishService;
        this.publishQueue = publishQueue;
        this.channelPersistence = channelPersistence;
        this.clusterExecutor = clusterExecutor;
        this.messageOrderingOoSZero = internalConfigurationService.getBoolean(Internals.MESSAGE_ORDERING_QOS_ZERO);
        eventBus.register(this);
    }

    @Subscribe
    @AllowConcurrentEvents
    public void onEvent(PluginOnPublish event) {
        if (this.callbackRegistry.isAvailable(OnAuthorizationCallback.class)) {
            this.eventBus.post(new PluginOnAuthorizationPublish(
                    event.getCtx(), event.getClientData(), event.getPublish()));
        } else {
            pluginOnPublishReceived(event.getClientData(), event.getPublish(),
                    event.getCtx());
        }
    }

    @Subscribe
    @AllowConcurrentEvents
    public void onEvent(PluginOnAuthorizationPublishCompleted event) {
        InternalPublish publish = event.getPublish();
        if (event.getBehaviour() == AuthorizationBehaviour.ACCEPT) {
            pluginOnPublishReceived(event.getClientData(), event.getPublish(), event.getCtx());
            return;
        }
        if (this.callbackRegistry.isAvailable(OnInsufficientPermissionDisconnect.class)) {
            event.getCtx().pipeline().fireUserEventTriggered(
                    new PluginOnInsufficientPermissionDisconnect(Type.PUBLISH, event.getClientData(),
                            publish.getTopic(), publish.getQoS()));
        }
        LOGGER.debug("Disconnecting client {} because of not authorized publish to topic {}",
                event.getClientData().getClientId(), publish.getTopic());
        event.getCtx().channel().close();
    }

    @Subscribe
    @AllowConcurrentEvents
    public void onEvent(PluginOnPublishReceivedCompleted event) {
        Channel channel = this.channelPersistence.getChannel(event.getClientData().getClientId());
        if (channel != null) {
            publish(event.getClientData(), event.getPublish(), channel.eventLoop());
        }
        publish(event.getClientData(), event.getPublish(), this.clusterExecutor);
    }

    private void pluginOnPublishReceived(ClientData clientData,
                                         InternalPublish publish,
                                         ChannelHandlerContext ctx) {
        if (this.callbackRegistry.isAvailable(OnPublishReceivedCallback.class)) {
            this.eventBus.post(new PluginOnPublishReceived(publish, clientData, ctx));
        } else {
            publish(clientData, publish, ctx.channel().eventLoop());
        }
    }

    private void publish(ClientData clientData, InternalPublish publish, ExecutorService executorService) {
        if (!this.messageOrderingOoSZero &&
                publish.getQoS() == QoS.AT_MOST_ONCE) {
            this.publishService.publish(publish, executorService);
        } else {
            this.publishQueue.dequeue(clientData.getClientId(), publish, executorService);
        }
    }
}
