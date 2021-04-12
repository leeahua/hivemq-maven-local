package bo;

import bm1.QueuedMessagesSinglePersistence;
import bu.InternalPublish;
import bx1.PluginOnPublishSend;
import cb1.AttributeKeys;
import cb1.ChannelUtils;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.hivemq.spi.annotations.Nullable;
import com.hivemq.spi.callback.events.OnPublishSend;
import com.hivemq.spi.callback.registry.CallbackRegistry;
import cs.ClientToken;
import i.ClusterConfigurationService;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

public class MqttPublishUserEventHandler extends ChannelInboundHandlerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(MqttPublishUserEventHandler.class);
    private final QueuedMessagesSinglePersistence queuedMessagesSinglePersistence;
    private final CallbackRegistry callbackRegistry;
    private final ClusterConfigurationService clusterConfigurationService;

    @Inject
    MqttPublishUserEventHandler(
            QueuedMessagesSinglePersistence queuedMessagesSinglePersistence,
            CallbackRegistry callbackRegistry,
            ClusterConfigurationService clusterConfigurationService) {
        this.queuedMessagesSinglePersistence = queuedMessagesSinglePersistence;
        this.callbackRegistry = callbackRegistry;
        this.clusterConfigurationService = clusterConfigurationService;
    }

    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        InternalPublish publish;
        SettableFuture<SendStatus> settableFuture;
        if (evt instanceof ClusterPublish) {
            publish = (ClusterPublish) evt;
            settableFuture = ((ClusterPublish) evt).getSettableFuture();
        } else if (evt instanceof InternalPublish) {
            publish = (InternalPublish) evt;
            settableFuture = null;
        } else {
            super.userEventTriggered(ctx, evt);
            return;
        }
        String clientId = ctx.channel().attr(AttributeKeys.MQTT_CLIENT_ID).get();
        if (this.clusterConfigurationService.isEnabled()) {
            retry(ctx, publish, settableFuture);
            return;
        }
        ListenableFuture<Boolean> future = this.queuedMessagesSinglePersistence.queuePublishIfQueueNotEmpty(clientId, publish);
        Futures.addCallback(future, new FutureCallback<Boolean>() {
            @Override
            public void onSuccess(@javax.annotation.Nullable Boolean result) {
                if (!result.booleanValue()) {
                    retry(ctx, publish, settableFuture);
                    return;
                }
                try {
                    if (settableFuture != null) {
                        settableFuture.set(SendStatus.QUEUED);
                    }
                    triggerEvent(ctx, evt);
                } catch (Exception t) {
                    throw new RuntimeException(t);
                }
            }

            @Override
            public void onFailure(Throwable t) {
                LOGGER.error("Error while queuing message", t);
            }
        }, ctx.executor());
    }

    private void triggerEvent(ChannelHandlerContext ctx, Object event) throws Exception {
        super.userEventTriggered(ctx, event);
    }

    private void retry(ChannelHandlerContext ctx, InternalPublish publish, @Nullable SettableFuture<SendStatus> settableFuture) {
        if (settableFuture == null) {
            ctx.writeAndFlush(publish);
        } else {
            ChannelPromise promise = ctx.channel().newPromise();
            ctx.writeAndFlush(publish, promise);
            promise.addListener(future -> {
                if (future.isSuccess()) {
                    settableFuture.set(SendStatus.DELIVERED);
                } else {
                    settableFuture.set(SendStatus.FAILED);
                }
            });
        }
        pluginOnPublishSend(ctx, publish);
    }

    private void pluginOnPublishSend(ChannelHandlerContext ctx, InternalPublish publish) {
        if (this.callbackRegistry.isAvailable(OnPublishSend.class)) {
            ClientToken clientToken = ChannelUtils.clientToken(ctx.channel());
            ctx.fireUserEventTriggered(new PluginOnPublishSend(clientToken, InternalPublish.of(publish)));
        }
    }
}
