package bj;

import am1.Metrics;
import av.HiveMQConfigurationService;
import av.Internals;
import bl1.ChannelPersistence;
import bm1.ClientSessionSinglePersistence;
import bm1.QueuedMessagesSinglePersistence;
import bn1.OutgoingMessageFlowSinglePersistence;
import br1.PluginAfterLogin;
import br1.PluginOnAuthentication;
import br1.PluginOnAuthenticationCompleted;
import br1.PluginRestrictionsAfterLogin;
import br1.PluginRestrictionsAfterLoginCompleted;
import bs1.AuthorizationType;
import bs1.PluginOnAuthorization;
import bs1.PluginOnAuthorizationCompleted;
import bu.InternalPublish;
import bu.MessageIDPools;
import bu1.PluginOnConnect;
import bu1.PluginOnConnectCompleted;
import ca.CallbackExecutor;
import cb1.AttributeKeys;
import cb1.ChannelUtils;
import ch.PluginAfterLoginCallbackHandler;
import ch.PluginOnAuthenticationCallbackHandler;
import ch.PluginRestrictionsCallbackHandler;
import cj.PluginOnConnectCallbackHandler;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.hivemq.spi.annotations.Nullable;
import com.hivemq.spi.callback.events.OnConnectCallback;
import com.hivemq.spi.callback.exception.AuthenticationException;
import com.hivemq.spi.callback.registry.CallbackRegistry;
import com.hivemq.spi.callback.security.AfterLoginCallback;
import com.hivemq.spi.callback.security.OnAuthenticationCallback;
import com.hivemq.spi.callback.security.OnAuthorizationCallback;
import com.hivemq.spi.callback.security.RestrictionsAfterLoginCallback;
import com.hivemq.spi.callback.security.authorization.AuthorizationBehaviour;
import com.hivemq.spi.message.ConnAck;
import com.hivemq.spi.message.Connect;
import com.hivemq.spi.message.ReturnCode;
import com.hivemq.spi.security.ClientCredentials;
import com.hivemq.spi.security.Restriction;
import com.hivemq.spi.security.RestrictionType;
import cs.ClientToken;
import e.Pipelines;
import i.ClusterIdProducer;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.handler.traffic.ChannelTrafficShapingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

@Singleton
@ChannelHandler.Sharable
public class MqttConnectHandler extends SimpleChannelInboundHandler<Connect> {
    private static final Logger LOGGER = LoggerFactory.getLogger(MqttConnectHandler.class);
    private final MqttDisallowSecondConnect disallowSecondConnect;
    private final ClientSessionSinglePersistence clientSessionSinglePersistence;
    private final QueuedMessagesSinglePersistence queuedMessagesSinglePersistence;
    private final ChannelPersistence channelPersistence;
    private final CallbackRegistry callbackRegistry;
    private final HiveMQConfigurationService hiveMQConfigurationService;
    private final Provider<PluginOnAuthenticationCallbackHandler> pluginOnAuthenticationCallbackHandlerProvider;
    private final Metrics metrics;
    private final OutgoingMessageFlowSinglePersistence outgoingMessageFlowSinglePersistence;
    private final CallbackExecutor callbackExecutor;
    private final MessageIDPools messageIDPools;
    private final ClusterIdProducer clusterIdProducer;
    private final MqttConnAckSentFutureListener connAckSentFutureListener = new MqttConnAckSentFutureListener();
    private int maxClientIdLength;

    @Inject
    public MqttConnectHandler(MqttDisallowSecondConnect disallowSecondConnect,
                              ClientSessionSinglePersistence clientSessionSinglePersistence,
                              QueuedMessagesSinglePersistence queuedMessagesSinglePersistence,
                              ChannelPersistence channelPersistence,
                              CallbackRegistry callbackRegistry,
                              HiveMQConfigurationService hiveMQConfigurationService,
                              Provider<PluginOnAuthenticationCallbackHandler> pluginOnAuthenticationCallbackHandlerProvider,
                              Metrics metrics,
                              OutgoingMessageFlowSinglePersistence outgoingMessageFlowSinglePersistence,
                              CallbackExecutor callbackExecutor,
                              MessageIDPools messageIDPools,
                              ClusterIdProducer clusterIdProducer) {
        this.disallowSecondConnect = disallowSecondConnect;
        this.clientSessionSinglePersistence = clientSessionSinglePersistence;
        this.queuedMessagesSinglePersistence = queuedMessagesSinglePersistence;
        this.channelPersistence = channelPersistence;
        this.callbackRegistry = callbackRegistry;
        this.hiveMQConfigurationService = hiveMQConfigurationService;
        this.pluginOnAuthenticationCallbackHandlerProvider = pluginOnAuthenticationCallbackHandlerProvider;
        this.metrics = metrics;
        this.outgoingMessageFlowSinglePersistence = outgoingMessageFlowSinglePersistence;
        this.callbackExecutor = callbackExecutor;
        this.messageIDPools = messageIDPools;
        this.clusterIdProducer = clusterIdProducer;
    }

    @PostConstruct
    public void init() {
        this.maxClientIdLength = this.hiveMQConfigurationService.mqttConfiguration().maxClientIdLength();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Connect msg) throws Exception {
        try {
            ctx.pipeline().addAfter(Pipelines.MQTT_MESSAGE_DECODER, Pipelines.MQTT_DISALLOW_SECOND_CONNECT, this.disallowSecondConnect);
        } catch (IllegalArgumentException e) {
            ctx.pipeline().firstContext().fireChannelRead(msg);
            return;
        }
        if (!validIdentifier(ctx, msg)) {
            return;
        }
        ctx.channel().attr(AttributeKeys.MQTT_TAKEN_OVER).set(false);
        removeConnectIdleHandler(ctx);
        pluginOnAuthentication(ctx, msg);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof PluginOnAuthenticationCompleted) {
            pluginOnAuthenticationCompleted(ctx, (PluginOnAuthenticationCompleted) evt);
        } else if (evt instanceof PluginRestrictionsAfterLoginCompleted) {
            pluginRestrictionsAfterLoginCompleted(ctx, (PluginRestrictionsAfterLoginCompleted) evt);
        } else if (evt instanceof PluginOnConnectCompleted) {
            pluginOnConnectCompleted(ctx, (PluginOnConnectCompleted) evt);
        } else if (evt instanceof PluginOnAuthorizationCompleted) {
            pluginOnAuthorizationCompleted(ctx, (PluginOnAuthorizationCompleted) evt);
        } else if (evt instanceof MqttConnectPersistenceHandler.OnConnectPersistenceCompleted) {
            MqttConnectPersistenceHandler.OnConnectPersistenceCompleted event = (MqttConnectPersistenceHandler.OnConnectPersistenceCompleted) evt;
            onConnectPersistenceCompleted(ctx, event.getConnect(), event.isSessionPresent());
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    private boolean validIdentifier(ChannelHandlerContext ctx, Connect connect) {
        if (connect.getClientIdentifier().length() <= this.maxClientIdLength) {
            return true;
        }
        LOGGER.trace("Client has sent a client identifier longer than {} characters, client disconnected", this.maxClientIdLength);
        writeRefusedConnAckToChannel(ctx, ReturnCode.REFUSED_IDENTIFIER_REJECTED);
        return false;
    }

    private void addTakenOverFutureListener(ChannelHandlerContext ctx, Connect connect) {
        ctx.channel().closeFuture().addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (!future.channel().attr(AttributeKeys.MQTT_TAKEN_OVER).get()) {
                    channelPersistence.remove(connect.getClientIdentifier());
                }
            }
        });
    }

    private void pluginOnAuthentication(ChannelHandlerContext ctx, Connect connect) {
        ClientToken clientToken = ChannelUtils.clientToken(ctx.channel());
        if (this.callbackRegistry.isAvailable(OnAuthenticationCallback.class)) {
            ctx.pipeline().addLast(Pipelines.PLUGIN_ON_AUTHENTICATION_CALLBACK_HANDLER, this.pluginOnAuthenticationCallbackHandlerProvider.get());
            ctx.fireUserEventTriggered(new PluginOnAuthentication(connect, clientToken));
        } else {
            pluginOnAuthorizationLWT(ctx, null, connect, clientToken, ReturnCode.ACCEPTED, true);
        }
    }

    private void pluginOnAuthenticationCompleted(ChannelHandlerContext ctx,
                                                 PluginOnAuthenticationCompleted event) {
        ReturnCode returnCode = event.getReturnCode();
        boolean accepted = returnCode == ReturnCode.ACCEPTED;
        pluginOnAuthorizationLWT(ctx, event.getException(), event.getConnect(),
                event.getClientCredentials(), returnCode, accepted);
    }

    private void pluginOnAuthorizationLWT(ChannelHandlerContext ctx,
                                          AuthenticationException exception,
                                          Connect connect,
                                          ClientCredentials clientCredentials,
                                          ReturnCode returnCode,
                                          boolean accepted) {
        if (connect.isWill() &&
                this.callbackRegistry.isAvailable(OnAuthorizationCallback.class)) {
            InternalPublish publish = createLWTPublish(connect);
            ctx.fireUserEventTriggered(new PluginOnAuthorization(AuthorizationType.LWT,
                    publish, clientCredentials, exception, connect, returnCode, accepted));
            return;
        }
        onAuthenticated(ctx, exception, connect, clientCredentials, returnCode, accepted);
    }

    private void pluginOnAuthorizationCompleted(
            ChannelHandlerContext ctx, PluginOnAuthorizationCompleted event) throws Exception {
        if (event.getType() != AuthorizationType.LWT) {
            super.userEventTriggered(ctx, event);
            return;
        }
        Connect connect = event.getConnect();
        ClientCredentials clientCredentials = (ClientCredentials) event.getClientData();
        if (event.getBehaviour() != AuthorizationBehaviour.ACCEPT) {
            LOGGER.info("Client [{}] is not permitted to publish to LWT topic [{}] with QoS [{}] and retain [{}], disconnected.",
                    event.getClientData().getClientId(), connect.getWillTopic(), connect.getWillQos(), connect.isWillRetain());
            writeRefusedConnAckToChannel(ctx, ReturnCode.REFUSED_NOT_AUTHORIZED);
            return;
        }
        onAuthenticated(ctx, event.getException(), connect, clientCredentials,
                event.getReturnCode(), event.isAccepted());
    }

    private void onAuthenticated(ChannelHandlerContext ctx,
                                 AuthenticationException exception,
                                 Connect connect,
                                 ClientCredentials clientCredentials,
                                 ReturnCode returnCode,
                                 boolean accepted) {
        ctx.pipeline().channel().attr(AttributeKeys.AUTHENTICATED).set(accepted);
        if (accepted) {
            ((ClientToken) clientCredentials).setAuthenticated(true);
        }
        pluginAfterLogin(ctx, exception, accepted);
        if (!accepted) {
            writeRefusedConnAckToChannel(ctx, returnCode);
            return;
        }
        pluginRestrictionsAfterLogin(ctx, connect, clientCredentials);
    }

    private void pluginAfterLogin(ChannelHandlerContext ctx,
                                  @Nullable AuthenticationException exception,
                                  boolean accepted) {
        if (this.callbackRegistry.isAvailable(AfterLoginCallback.class)) {
            ctx.pipeline().addLast(Pipelines.PLUGIN_AFTER_LOGIN_CALLBACK_HANDLER,
                    new PluginAfterLoginCallbackHandler(this.callbackRegistry, this.metrics, this.callbackExecutor));
            ctx.fireUserEventTriggered(new PluginAfterLogin(ChannelUtils.clientToken(ctx.channel()), accepted, exception));
        }
    }

    private void pluginRestrictionsAfterLogin(ChannelHandlerContext ctx,
                                              Connect connect,
                                              ClientCredentials clientCredentials) {
        if (this.callbackRegistry.isAvailable(RestrictionsAfterLoginCallback.class)) {
            ctx.pipeline().addLast(Pipelines.PLUGIN_RESTRICTIONS_CALLBACK_HANDLER,
                    new PluginRestrictionsCallbackHandler(this.callbackRegistry, this.metrics, this.callbackExecutor));
            ctx.fireUserEventTriggered(new PluginRestrictionsAfterLogin(connect, clientCredentials));
        } else {
            pluginOnConnect(ctx, connect);
        }
    }

    private void pluginRestrictionsAfterLoginCompleted(ChannelHandlerContext ctx,
                                                       PluginRestrictionsAfterLoginCompleted event) {
        boolean hasLimit = false;
        long writeLimit = 0L;
        long readLimit = 0L;
        Iterator<Restriction> restrictionIterator = event.getRestrictions().iterator();
        while (restrictionIterator.hasNext()) {
            Restriction restriction = restrictionIterator.next();
            if (restriction.getType() == RestrictionType.MAX_PUBLISH_MESSAGE_SIZE) {
                ctx.channel().attr(AttributeKeys.RESTRICTION_MAX_PUBLISH_MESSAGE_SIZE)
                        .set(restriction.getValue());
                continue;
            }
            if (restriction.getType() == RestrictionType.MAX_OUTGOING_BYTES_SEC) {
                ctx.channel().attr(AttributeKeys.RESTRICTION_MAX_OUTGOING_BYTES_SEC)
                        .set(restriction.getValue());
                if (restriction.getValue() > 0L) {
                    writeLimit = restriction.getValue();
                    hasLimit = true;
                }
                continue;
            }
            if (restriction.getType() == RestrictionType.MAX_INCOMING_BYTES) {
                ctx.channel().attr(AttributeKeys.RESTRICTION_MAX_INCOMING_BYTES_SEC)
                        .set(restriction.getValue());
                if (restriction.getValue() > 0L) {
                    readLimit = restriction.getValue();
                    hasLimit = true;
                }
            }
        }
        if (hasLimit) {
            ctx.channel().pipeline().addAfter(Pipelines.GLOBAL_THROTTLING_HANDLER, Pipelines.CHANNEL_THROTTLING_HANDLER,
                    new ChannelTrafficShapingHandler(writeLimit, readLimit));
        }
        pluginOnConnect(ctx, event.getConnect());
    }

    private void pluginOnConnect(ChannelHandlerContext ctx, Connect paramConnect) {
        if (this.callbackRegistry.isAvailable(OnConnectCallback.class)) {
            ctx.pipeline().addLast(Pipelines.PLUGIN_ON_CONNECT_CALLBACK_HANDLER,
                    new PluginOnConnectCallbackHandler(this.callbackRegistry, this.metrics, this.callbackExecutor));
            ctx.fireUserEventTriggered(new PluginOnConnect(paramConnect, ChannelUtils.clientToken(ctx.channel())));
        } else {
            onConnected(ctx, paramConnect);
        }
    }

    private void pluginOnConnectCompleted(ChannelHandlerContext ctx,
                                          PluginOnConnectCompleted event) {
        if (event.isRefused()) {
            writeRefusedConnAckToChannel(ctx,
                    event.getReturnCode() != null ? event.getReturnCode() : ReturnCode.REFUSED_NOT_AUTHORIZED);
            return;
        }
        onConnected(ctx, event.getConnect());
    }

    private void onConnected(ChannelHandlerContext ctx, Connect connect) {
        afterLogin(ctx, connect);
    }

    protected void afterLogin(ChannelHandlerContext ctx, Connect connect) {
        disconnectConnectedClientWithSameId(connect);
        this.channelPersistence.persist(connect.getClientIdentifier(), ctx.channel());
        addTakenOverFutureListener(ctx, connect);
        if (connect.isCleanSession()) {
            ctx.fireUserEventTriggered(new MqttConnectPersistenceHandler.OnConnectPersistence(connect, false));
            return;
        }
        this.metrics.persistentSessionsActive().inc();
        addPersistentSessionsInActiveListener(ctx);
        ListenableFuture<Boolean> future = this.clientSessionSinglePersistence.isPersistent(connect.getClientIdentifier());
        Futures.addCallback(future, new FutureCallback<Boolean>() {
            @Override
            public void onSuccess(@javax.annotation.Nullable Boolean result) {
                ctx.fireUserEventTriggered(new MqttConnectPersistenceHandler.OnConnectPersistence(connect, result));
            }

            @Override
            public void onFailure(Throwable t) {
                t.printStackTrace();
            }
        }, ctx.executor());
    }

    protected void onConnectPersistenceCompleted(ChannelHandlerContext ctx,
                                                 Connect connect,
                                                 boolean sessionPresent) {
        addKeepAliveHandlerForClient(ctx, connect);
        writeConnAckToChannel(ctx, connect, sessionPresent);
        try {
            ctx.pipeline().remove(this);
        } catch (NoSuchElementException localNoSuchElementException) {
        }
        ctx.fireChannelRead(connect);
    }

    private void addPersistentSessionsInActiveListener(ChannelHandlerContext ctx) {
        ctx.channel().closeFuture().addListener(future ->
                metrics.persistentSessionsActive().dec()
        );
    }

    private void writeConnAckToChannel(ChannelHandlerContext ctx,
                                       Connect connect,
                                       boolean sessionPresent) {
        ChannelFuture future;
        if (sessionPresent) {
            future = ctx.writeAndFlush(ConnAcks.ACCEPTED_AND_SESSION_PRESENT);
        } else {
            future = ctx.writeAndFlush(ConnAcks.ACCEPTED);
        }
        future.addListener(this.connAckSentFutureListener);
        if (!connect.isCleanSession()) {
            future.addListener(
                    new SendOutMessagesFutureListener(this.queuedMessagesSinglePersistence, this.outgoingMessageFlowSinglePersistence,
                            ctx.channel().attr(AttributeKeys.MQTT_CLIENT_ID).get(),
                            this.messageIDPools, this.channelPersistence));
        }
    }

    protected void writeRefusedConnAckToChannel(ChannelHandlerContext ctx, ReturnCode returnCode) {
        ConnAck connAck = ConnAcks.refused(returnCode);
        ctx.writeAndFlush(connAck).addListener(ChannelFutureListener.CLOSE);
    }

    private void disconnectConnectedClientWithSameId(Connect connect) {
        Channel channel = this.channelPersistence.getChannel(connect.getClientIdentifier());
        if (channel != null) {
            LOGGER.debug("Disconnecting already connected client with id {} because another client connects with that id", connect.getClientIdentifier());
            channel.attr(AttributeKeys.MQTT_TAKEN_OVER).set(true);
            channel.close();
        }
    }

    private void removeConnectIdleHandler(ChannelHandlerContext ctx) {
        try {
            ctx.pipeline().remove(Pipelines.NEW_CONNECTION_IDLE_HANDLER);
            ctx.pipeline().remove(Pipelines.NO_CONNECT_IDLE_EVENT_HANDLER);
        } catch (NoSuchElementException e) {
            LOGGER.trace("Not able to remove no connect idle handler");
        }
    }

    private void addKeepAliveHandlerForClient(ChannelHandlerContext ctx, Connect connect) {
        if (connect.getKeepAliveTimer() > 0) {
            Double timeout = connect.getKeepAliveTimer() * getKeepAliveFactor();
            LOGGER.trace("Client specified a keepAlive value of {}s. The maximum timeout before disconnecting is {}s",
                    connect.getKeepAliveTimer(), timeout);
            ctx.pipeline().addFirst(Pipelines.MQTT_KEEPALIVE_IDLE_NOTIFIER_HANDLER, new IdleStateHandler(timeout.longValue(), 0L, 0L, TimeUnit.SECONDS));
            ctx.pipeline().addAfter(Pipelines.MQTT_KEEPALIVE_IDLE_NOTIFIER_HANDLER, Pipelines.MQTT_KEEPALIVE_IDLE_HANDLER, new MqttKeepAliveIdleHandler());
        }
    }

    private double getKeepAliveFactor() {
        return this.hiveMQConfigurationService.internalConfiguration()
                .getDouble(Internals.MQTT_CONNECTION_KEEP_ALIVE_FACTOR);
    }


    private InternalPublish createLWTPublish(Connect connect) {
        InternalPublish publish = new InternalPublish(this.clusterIdProducer.get());
        publish.setPayload(connect.getWillMessage());
        publish.setTopic(connect.getWillTopic());
        publish.setRetain(connect.isWillRetain());
        publish.setQoS(connect.getWillQos());
        return publish;
    }


}
