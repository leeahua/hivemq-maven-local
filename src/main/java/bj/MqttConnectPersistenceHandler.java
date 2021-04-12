package bj;

import aj.ClusterFutures;
import am1.Metrics;
import bm1.ClientSessionSinglePersistence;
import bm1.ClientSessionSubscriptionsSinglePersistence;
import bn1.OutgoingMessageFlowClusterPersistence;
import bn1.OutgoingMessageFlowSinglePersistence;
import bu.MessageIDPools;
import bv.MessageIdProducer;
import cb1.PluginExceptionUtils;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.hivemq.spi.annotations.NotNull;
import u1.MessageFlow;
import com.hivemq.spi.message.Connect;
import i.ClusterConfigurationService;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
// TODO:
@Singleton
@ChannelHandler.Sharable
public class MqttConnectPersistenceHandler extends ChannelInboundHandlerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(MqttConnectPersistenceHandler.class);
    private final ClientSessionSinglePersistence clientSessionSinglePersistence;
    private final ClientSessionSubscriptionsSinglePersistence clientSessionSubscriptionsSinglePersistence;
    private final Metrics metrics;
    private final MessageIDPools messageIDPools;
    private final OutgoingMessageFlowClusterPersistence outgoingMessageFlowClusterPersistence;
    private final OutgoingMessageFlowSinglePersistence outgoingMessageFlowSinglePersistence;
    private final ClusterConfigurationService clusterConfigurationService;

    @Inject
    MqttConnectPersistenceHandler(ClientSessionSinglePersistence clientSessionSinglePersistence,
                                  ClientSessionSubscriptionsSinglePersistence clientSessionSubscriptionsSinglePersistence,
                                  Metrics metrics,
                                  MessageIDPools messageIDPools,
                                  OutgoingMessageFlowClusterPersistence outgoingMessageFlowClusterPersistence,
                                  OutgoingMessageFlowSinglePersistence outgoingMessageFlowSinglePersistence,
                                  ClusterConfigurationService clusterConfigurationService) {
        this.clientSessionSinglePersistence = clientSessionSinglePersistence;
        this.clientSessionSubscriptionsSinglePersistence = clientSessionSubscriptionsSinglePersistence;
        this.metrics = metrics;
        this.messageIDPools = messageIDPools;
        this.outgoingMessageFlowClusterPersistence = outgoingMessageFlowClusterPersistence;
        this.outgoingMessageFlowSinglePersistence = outgoingMessageFlowSinglePersistence;
        this.clusterConfigurationService = clusterConfigurationService;
    }

    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (!(evt instanceof OnConnectPersistence)) {
            super.userEventTriggered(ctx, evt);
            return;
        }
        OnConnectPersistence event = (OnConnectPersistence) evt;
        ListenableFuture<Void> future = persistent(!event.getConnect().isCleanSession(), event.getConnect().getClientIdentifier());
        ClusterFutures.addCallback(future, new ResultCallback(ctx, event, this));
        addCloseFutureListener(ctx, event.getConnect().getClientIdentifier(), !event.getConnect().isCleanSession());
    }

    private ListenableFuture<Void> persistent(boolean persistSession, String clientId) {
        SettableFuture<Void> settableFuture = SettableFuture.create();
        ListenableFuture<MessageFlow> future = this.clientSessionSinglePersistence.persistent(clientId, persistSession);
        ClusterFutures.addCallback(future, new FutureCallback<MessageFlow>() {
            @Override
            public void onSuccess(@Nullable MessageFlow result) {
                if (result == null) {
                    settableFuture.set(null);
                }
                List<ListenableFuture<Void>> futures = new ArrayList<>();
                result.getMessages().forEach(messageWithId -> {
                    MessageIdProducer producer = messageIDPools.forClient(clientId);
                    producer.add(messageWithId.getMessageId());
                    ListenableFuture<Void> future = outgoingMessageFlowSinglePersistence.addOrReplace(clientId, messageWithId.getMessageId(), messageWithId);
                    futures.add(future);
                });
                ListenableFuture<Void> future = ClusterFutures.merge(futures);
                ClusterFutures.setFuture(future, settableFuture);
            }

            @Override
            public void onFailure(Throwable t) {
                LOGGER.error("Exception while receiving message flow entries for client: {}.", clientId, t);
                settableFuture.setException(t);
            }
        });
        return settableFuture;
    }

    private void addCloseFutureListener(ChannelHandlerContext ctx, String clientId, boolean persistSession) {
        ctx.channel().closeFuture().addListener(new CloseFutureListener(clientId, persistSession));
    }


    private class CloseFutureListener implements ChannelFutureListener {
        private final String clientId;
        private final boolean persistSession;

        public CloseFutureListener(String clientId, boolean persistSession) {
            this.clientId = clientId;
            this.persistSession = persistSession;
        }

        @Override
        public void operationComplete(ChannelFuture channelFuture) throws Exception {
            ListenableFuture<Void> future = clientSessionSinglePersistence.disconnected(this.clientId);
            ClusterFutures.addCallback(future, new FutureCallback<Void>() {

                @Override
                public void onSuccess(@Nullable Void result) {
                    ListenableFuture<Void> future = outgoingMessageFlowClusterPersistence.put(clientId);
                    if (!persistSession) {
                        return;
                    }
                    ClusterFutures.addCallback(future, new FutureCallback<Void>() {
                        @Override
                        public void onSuccess(@Nullable Void result) {
                            outgoingMessageFlowSinglePersistence.removeForClient(clientId);
                        }

                        @Override
                        public void onFailure(Throwable t) {
                            LOGGER.error("Exception while sending outgoing message flow put request.", t);
                        }
                    });
                }

                @Override
                public void onFailure(Throwable t) {
                    PluginExceptionUtils.logOrThrow("Unable to update client session data for disconnecting client " +
                            clientId + " with clean session set to " +
                            !persistSession + ".", t);
                }
            });
        }
    }

    private static class ResultCallback implements FutureCallback<Void> {
        private final ChannelHandlerContext ctx;
        private final OnConnectPersistence event;
        private final MqttConnectPersistenceHandler handler;

        public ResultCallback(ChannelHandlerContext ctx,
                              OnConnectPersistence event,
                              MqttConnectPersistenceHandler handler) {
            this.ctx = ctx;
            this.event = event;
            this.handler = handler;
        }

        @Override
        public void onSuccess(@Nullable Void result) {
            this.ctx.channel().pipeline().fireUserEventTriggered(
                    new OnConnectPersistenceCompleted(this.event.getConnect(), this.event.isSessionPresent()));
            if (this.ctx.pipeline().get(MqttConnectPersistenceHandler.class) != null) {
                this.ctx.pipeline().remove(this.handler);
            }
        }

        public void onFailure(Throwable t) {
            PluginExceptionUtils.logOrThrow("Unable to handle client connection for id " +
                    this.event.getConnect().getClientIdentifier() + ".", t);
            this.ctx.channel().disconnect();
        }
    }

    public static class OnConnectPersistenceCompleted {
        private final Connect connect;
        private final boolean sessionPresent;

        public OnConnectPersistenceCompleted(@NotNull Connect connect, boolean sessionPresent) {
            this.connect = connect;
            this.sessionPresent = sessionPresent;
        }

        public Connect getConnect() {
            return connect;
        }

        public boolean isSessionPresent() {
            return sessionPresent;
        }
    }

    public static class OnConnectPersistence {
        private final Connect connect;
        private final boolean sessionPresent;

        public OnConnectPersistence(@NotNull Connect connect, boolean sessionPresent) {
            this.connect = connect;
            this.sessionPresent = sessionPresent;
        }

        public Connect getConnect() {
            return connect;
        }

        public boolean isSessionPresent() {
            return sessionPresent;
        }
    }
}
