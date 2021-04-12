package bs;

import aj.ClusterFutures;
import am1.Metrics;
import bm1.ClientSessionSubscriptionsSinglePersistence;
import bz1.PluginOnUnsubscribe;
import cb1.AttributeKeys;
import cb1.ChannelUtils;
import cb1.PluginExceptionUtils;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.hivemq.spi.callback.events.OnUnsubscribeCallback;
import com.hivemq.spi.callback.registry.CallbackRegistry;
import com.hivemq.spi.message.Topic;
import com.hivemq.spi.message.UnsubAck;
import com.hivemq.spi.message.Unsubscribe;
import cs.ClientToken;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

public class MqttUnsubscribeHandler
        extends SimpleChannelInboundHandler<Unsubscribe> {
    private static final Logger LOGGER = LoggerFactory.getLogger(MqttUnsubscribeHandler.class);
    private final ClientSessionSubscriptionsSinglePersistence clientSessionSubscriptionsSinglePersistence;
    private final CallbackRegistry callbackRegistry;
    private final Metrics metrics;

    @Inject
    public MqttUnsubscribeHandler(ClientSessionSubscriptionsSinglePersistence clientSessionSubscriptionsSinglePersistence,
                                  CallbackRegistry callbackRegistry,
                                  Metrics metrics) {
        this.clientSessionSubscriptionsSinglePersistence = clientSessionSubscriptionsSinglePersistence;
        this.callbackRegistry = callbackRegistry;
        this.metrics = metrics;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Unsubscribe msg) throws Exception {
        String clientId = ctx.channel().attr(AttributeKeys.MQTT_CLIENT_ID).get();
        List<ListenableFuture<Void>> futures = new ArrayList<>();
        msg.getTopics().stream()
                .map(Topic::topicFromString)
                .forEach(topic -> {
                    futures.add(this.clientSessionSubscriptionsSinglePersistence.removeSubscription(clientId, topic));
                    this.metrics.subscriptionsCurrent().dec();
                    LOGGER.trace("Unsubscribed from topic [{}] for client [{}]", topic, clientId);
                });
        LOGGER.trace("Applied all unsubscriptions for client [{}]", clientId);
        ListenableFuture<Void> future = ClusterFutures.merge(futures);
        Futures.addCallback(future, new FutureCallback<Void>() {

            @Override
            public void onSuccess(@Nullable Void result) {
                ctx.writeAndFlush(new UnsubAck(msg.getMessageId()));
                pluginOnUnsubscribe(ctx, msg);
            }

            @Override
            public void onFailure(Throwable t) {
                PluginExceptionUtils.logOrThrow("Unable to unsubscribe client " + clientId + ".", t);
            }
        }, ctx.executor());
    }

    private void pluginOnUnsubscribe(ChannelHandlerContext ctx, Unsubscribe unsubscribe) {
        if (this.callbackRegistry.isAvailable(OnUnsubscribeCallback.class)) {
            ClientToken clientToken = ChannelUtils.clientToken(ctx.channel());
            ctx.fireUserEventTriggered(new PluginOnUnsubscribe(clientToken, unsubscribe));
        }
    }


}
