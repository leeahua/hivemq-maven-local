package bq;

import aj.ClusterFutures;
import am1.Metrics;
import bm1.ClientSessionSubscriptionsSinglePersistence;
import br.SubAckSendFutureListener;
import bs1.AuthorizationType;
import bs1.PluginOnAuthorization;
import bs1.PluginOnAuthorizationCompleted;
import bs1.PluginOnInsufficientPermissionDisconnect;
import bs1.PluginOnInsufficientPermissionDisconnect.Type;
import bu.MessageIDPools;
import by1.PluginOnSubscribe;
import by1.PluginOnSubscribeCompleted;
import cb1.AttributeKeys;
import cb1.ChannelUtils;
import cb1.PluginExceptionUtils;
import cb1.WildcardValidationUtils;
import com.google.common.primitives.Bytes;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.hivemq.spi.callback.events.OnSubscribeCallback;
import com.hivemq.spi.callback.registry.CallbackRegistry;
import com.hivemq.spi.callback.security.OnAuthorizationCallback;
import com.hivemq.spi.callback.security.OnInsufficientPermissionDisconnect;
import com.hivemq.spi.callback.security.authorization.AuthorizationBehaviour;
import com.hivemq.spi.message.ProtocolVersion;
import com.hivemq.spi.message.SubAck;
import com.hivemq.spi.message.Subscribe;
import com.hivemq.spi.message.Topic;
import com.hivemq.spi.security.ClientData;
import cs.ClientToken;
import i.ClusterIdProducer;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.concurrent.EventExecutorGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import x.RetainedMessagesSinglePersistence;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MqttSubscribeHandler extends SimpleChannelInboundHandler<Subscribe> {
    private static final Logger LOGGER = LoggerFactory.getLogger(MqttSubscribeHandler.class);
    private final ClientSessionSubscriptionsSinglePersistence clientSessionSubscriptionsSinglePersistence;
    private final RetainedMessagesSinglePersistence retainedMessagesSinglePersistence;
    private final CallbackRegistry callbackRegistry;
    private final Metrics metrics;
    private final EventExecutorGroup eventExecutorGroup;
    private final MessageIDPools messageIDPools;
    private final ClusterIdProducer clusterIdProducer;

    @Inject
    MqttSubscribeHandler(ClientSessionSubscriptionsSinglePersistence clientSessionSubscriptionsSinglePersistence,
                         RetainedMessagesSinglePersistence retainedMessagesSinglePersistence,
                         CallbackRegistry callbackRegistry,
                         Metrics metrics,
                         EventExecutorGroup eventExecutorGroup,
                         MessageIDPools messageIDPools,
                         ClusterIdProducer clusterIdProducer) {
        this.clientSessionSubscriptionsSinglePersistence = clientSessionSubscriptionsSinglePersistence;
        this.retainedMessagesSinglePersistence = retainedMessagesSinglePersistence;
        this.callbackRegistry = callbackRegistry;
        this.metrics = metrics;
        this.eventExecutorGroup = eventExecutorGroup;
        this.messageIDPools = messageIDPools;
        this.clusterIdProducer = clusterIdProducer;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Subscribe msg) throws Exception {
        if (!validSubscribeTopics(ctx, msg)) {
            return;
        }
        ClientToken clientToken = ChannelUtils.clientToken(ctx.channel());
        if (this.callbackRegistry.isAvailable(OnAuthorizationCallback.class)) {
            ctx.fireUserEventTriggered(new PluginOnAuthorization(msg, clientToken));
        } else {
            pluginOnSubscribe(ctx, msg, clientToken, null);
        }
    }

    private boolean validSubscribeTopics(ChannelHandlerContext ctx, Subscribe subscribe) {
        LOGGER.trace("Checking Subscribe message of client '{}' if topics are valid",
                ChannelUtils.clientToken(ctx.channel()).getClientId());
        Optional<Topic> mayInvalidTopic = subscribe.getTopics().stream()
                .filter(topic -> !WildcardValidationUtils.validSubscribeTopic(topic.getTopic()))
                .findFirst();
        if (!mayInvalidTopic.isPresent()) {
            return true;
        }
        Topic invalidTopic = mayInvalidTopic.get();
        LOGGER.debug("Disconnecting client '{}' because it sent an invalid subscription: '{}'",
                ChannelUtils.clientToken(ctx.channel()).getClientId(), invalidTopic.getTopic());
        ctx.close();
        return false;
    }

    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof PluginOnSubscribeCompleted) {
            pluginOnSubscribeCompleted(ctx, (PluginOnSubscribeCompleted) evt);
        } else if (evt instanceof PluginOnAuthorizationCompleted) {
            pluginOnAuthorizationCompleted(ctx, (PluginOnAuthorizationCompleted) evt);
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    private void pluginOnSubscribe(ChannelHandlerContext ctx, Subscribe subscribe,
                                   ClientData clientData, byte[] grantedQoSNumbers) {
        if (this.callbackRegistry.isAvailable(OnSubscribeCallback.class)) {
            ctx.fireUserEventTriggered(new PluginOnSubscribe(subscribe, clientData, grantedQoSNumbers));
        } else {
            subscribe(ctx, clientData, subscribe, grantedQoSNumbers);
        }
    }

    private void pluginOnSubscribeCompleted(ChannelHandlerContext ctx,
                                            PluginOnSubscribeCompleted event) {
        subscribe(ctx, event.getClientData(), event.getSubscribe(), event.getGrantedQoSNumbers());
    }

    private void pluginOnAuthorizationCompleted(ChannelHandlerContext ctx, PluginOnAuthorizationCompleted event) throws Exception {
        Subscribe subscribe = event.getSubscribe();
        if (subscribe == null ||
                event.getType() != AuthorizationType.SUBSCRIBE) {
            super.userEventTriggered(ctx, event);
            return;
        }
        if (subscribe.getTopics() == null) {
            throw new IllegalArgumentException("Subscribe packet has no topics");
        }
        byte[] grantedQoSNumbers = getGrantedQoSNumbers(event.getBehaviours(), subscribe);
        pluginOnSubscribe(ctx, event.getSubscribe(), event.getClientData(), grantedQoSNumbers);
    }

    private byte[] getGrantedQoSNumbers(List<AuthorizationBehaviour> behaviours, Subscribe subscribe) {
        if (behaviours == null) {
            return null;
        }
        int topicCount = subscribe.getTopics().size();
        if (topicCount <= 0) {
            throw new IllegalArgumentException("Subscribe packet has no topics");
        }
        byte[] grantedQoSNumbers = new byte[topicCount];
        for (int index = 0; index < topicCount; index++) {
            Topic topic = subscribe.getTopics().get(index);
            if (index > behaviours.size() - 1) {
                grantedQoSNumbers[index] = Byte.MIN_VALUE;
                continue;
            }
            AuthorizationBehaviour behaviour = behaviours.get(index);
            if (behaviour != AuthorizationBehaviour.ACCEPT) {
                grantedQoSNumbers[index] = Byte.MIN_VALUE;
            } else {
                grantedQoSNumbers[index] = ((byte) topic.getQoS().getQosNumber());
            }
        }
        return grantedQoSNumbers;
    }

    private void subscribe(ChannelHandlerContext ctx,
                           ClientData clientData,
                           Subscribe subscribe,
                           byte[] grantedQoSNumbers) {
        this.eventExecutorGroup.submit(() -> {
            String clientId = ctx.channel().attr(AttributeKeys.MQTT_CLIENT_ID).get();
            ProtocolVersion protocolVersion = ctx.channel().attr(AttributeKeys.MQTT_VERSION).get();
            byte[] grantedQoS = new byte[subscribe.getTopics().size()];
            List<ListenableFuture<Void>> subscribeFutures = new ArrayList<>();
            for (int index = 0; index < subscribe.getTopics().size(); index++) {
                final int currentIndex = index;
                Topic topic = subscribe.getTopics().get(index);
                grantedQoS[index] = grantedQoSNumbers == null ?
                        (byte) topic.getQoS().getQosNumber() : grantedQoSNumbers[index];
                if (grantedQoS[index] == Byte.MIN_VALUE) {
                    if (protocolVersion == ProtocolVersion.MQTTv3_1) {
                        disconnectChannelWhenNotPermitted(ctx, clientData, topic);
                        return;
                    }
                    LOGGER.trace("Ignoring subscription for client [{}] and topic [{}] with qos [{}] because the client is not permitted",
                            clientId, topic.getTopic(), topic.getQoS());
                    continue;
                }
                SettableFuture<Void> settableFuture = SettableFuture.create();
                subscribeFutures.add(settableFuture);
                ListenableFuture<Void> future = clientSessionSubscriptionsSinglePersistence.addSubscription(clientId, topic);
                ClusterFutures.addCallback(future, new FutureCallback<Void>() {

                    @Override
                    public void onSuccess(@Nullable Void result) {
                        settableFuture.set(null);
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        if (protocolVersion == ProtocolVersion.MQTTv3_1_1) {
                            PluginExceptionUtils.logOrThrow("Unable to persist subscription to topic "
                                    + topic.getTopic() + " for client " + clientId + ".", t);
                            grantedQoS[currentIndex] = Byte.MIN_VALUE;
                            settableFuture.set(null);
                        } else {
                            settableFuture.setException(t);
                        }
                    }
                });
                LOGGER.trace("Adding subscriptions for client [{}] and topic [{}] with qos [{}]",
                        clientId, topic.getTopic(), topic.getQoS());
                metrics.subscriptionsCurrent().inc();
            }
            LOGGER.trace("Applied all subscriptions for client [{}]", clientId);
            if(subscribeFutures.isEmpty()){
                ChannelFuture future = ctx.writeAndFlush(new SubAck(subscribe.getMessageId(), Bytes.asList(grantedQoS)));
                future.addListener(new SubAckSendFutureListener(
                        subscribe.getTopics(), retainedMessagesSinglePersistence, messageIDPools, clusterIdProducer));
                return;
            }
            ListenableFuture<Void> future = ClusterFutures.merge(subscribeFutures);
            Futures.addCallback(future, new FutureCallback<Void>() {

                @Override
                public void onSuccess(@Nullable Void result) {
                    ChannelFuture channelFuture = ctx.writeAndFlush(
                            new SubAck(subscribe.getMessageId(), Bytes.asList(grantedQoS)));
                    channelFuture.addListener(new SubAckSendFutureListener(
                            subscribe.getTopics(), retainedMessagesSinglePersistence, messageIDPools, clusterIdProducer));
                }

                @Override
                public void onFailure(Throwable t) {
                    ctx.channel().disconnect();
                }
            }, ctx.executor());
        });
    }


    private void disconnectChannelWhenNotPermitted(ChannelHandlerContext ctx,
                                                   ClientData clientData,
                                                   Topic topic) {
        LOGGER.debug("Disconnecting MQTT v3.1 client [{}] because subscribing to topic [{}] with qos [{}] is not permitted",
                clientData.getClientId(), topic.getTopic(), topic.getQoS());
        ctx.channel().close();
        if (this.callbackRegistry.isAvailable(OnInsufficientPermissionDisconnect.class)) {
            ctx.fireUserEventTriggered(new PluginOnInsufficientPermissionDisconnect(Type.SUBSCRIBE, clientData, topic.getTopic(), topic.getQoS()));
        }
    }


}
