package cj;

import am1.Metrics;
import bu1.PluginOnConnect;
import bu1.PluginOnConnectCallbackCompleted;
import bu1.PluginOnConnectCompleted;
import ca.CallableTask;
import ca.CallbackExecutor;
import cb1.PluginExceptionUtils;
import ch.PluginOnAuthenticationCallbackHandler;
import com.codahale.metrics.Timer;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.hivemq.spi.annotations.NotNull;
import com.hivemq.spi.callback.events.OnConnectCallback;
import com.hivemq.spi.callback.exception.RefusedConnectionException;
import com.hivemq.spi.callback.registry.CallbackRegistry;
import com.hivemq.spi.message.Connect;
import com.hivemq.spi.message.ReturnCode;
import com.hivemq.spi.security.ClientData;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Queue;

@Singleton
@ChannelHandler.Sharable
public class PluginOnConnectCallbackHandler extends ChannelInboundHandlerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(PluginOnAuthenticationCallbackHandler.class);
    private final CallbackRegistry callbackRegistry;
    private final Metrics metrics;
    private final CallbackExecutor callbackExecutor;

    @Inject
    public PluginOnConnectCallbackHandler(CallbackRegistry callbackRegistry,
                                          Metrics metrics,
                                          CallbackExecutor callbackExecutor) {
        this.callbackRegistry = callbackRegistry;
        this.metrics = metrics;
        this.callbackExecutor = callbackExecutor;
    }

    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof PluginOnConnect) {
            onConnect(ctx, (PluginOnConnect) evt);
        } else if (evt instanceof PluginOnConnectCallbackCompleted) {
            onConnectCallbackCompleted(ctx, (PluginOnConnectCallbackCompleted) evt);
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    private void onConnect(ChannelHandlerContext ctx, PluginOnConnect event) {
        boolean available = this.callbackRegistry.isAvailable(OnConnectCallback.class);
        if (!available) {
            ctx.pipeline().fireUserEventTriggered(new PluginOnConnectCompleted(event.getClientData(), event.getConnect(), false, null));
            return;
        }
        Deque<OnConnectCallback> leftCallbacks = new ArrayDeque(this.callbackRegistry.getCallbacks(OnConnectCallback.class));
        ClientData clientData = event.getClientData();
        OnConnectCallback callback = leftCallbacks.poll();
        submitTask(ctx, callback, clientData, event.getConnect(), leftCallbacks);
    }

    private void onConnectCallbackCompleted(ChannelHandlerContext ctx,
                                            PluginOnConnectCallbackCompleted event) {
        Connect connect = event.getConnect();
        ClientData clientData = event.getClientData();
        if (event.isRefused()) {
            ctx.pipeline().fireUserEventTriggered(
                    new PluginOnConnectCompleted(clientData, connect, true, event.getReturnCode()));
            ctx.pipeline().remove(this);
            return;
        }
        if (event.getLeftCallbacks().size() == 0) {
            ctx.pipeline().fireUserEventTriggered(
                    new PluginOnConnectCompleted(clientData, connect, false, null));
            ctx.pipeline().remove(this);
            return;
        }
        Queue<OnConnectCallback> leftCallbacks = event.getLeftCallbacks();
        OnConnectCallback callback = leftCallbacks.poll();
        submitTask(ctx, callback, clientData, connect, leftCallbacks);
    }

    private void submitTask(ChannelHandlerContext ctx,
                            OnConnectCallback callback,
                            ClientData clientData,
                            Connect connect,
                            Queue<OnConnectCallback> leftCallbacks) {
        ListenableFuture localListenableFuture = this.callbackExecutor.submit(
                createTask(callback, clientData, connect));
        Futures.addCallback(localListenableFuture,
                createResultCallback(ctx, clientData, connect, leftCallbacks),
                ctx.executor().parent());
    }

    @NotNull
    @VisibleForTesting
    Task createTask(OnConnectCallback callback,
                    ClientData clientData,
                    Connect connect) {
        return new Task(callback, clientData, connect, this.metrics);
    }

    @NotNull
    @VisibleForTesting
    ResultCallback createResultCallback(ChannelHandlerContext ctx,
                                        ClientData clientData,
                                        Connect connect,
                                        Queue<OnConnectCallback> leftCallbacks) {
        return new ResultCallback(ctx, clientData, connect, leftCallbacks);
    }

    @VisibleForTesting
    static class ResultCallback implements FutureCallback<PluginOnConnectResult> {
        private final ChannelHandlerContext ctx;
        private final ClientData clientData;
        private final Connect connect;
        private final Queue<OnConnectCallback> leftCallbacks;

        public ResultCallback(ChannelHandlerContext ctx,
                              ClientData clientData,
                              Connect connect,
                              Queue<OnConnectCallback> leftCallbacks) {
            this.ctx = ctx;
            this.clientData = clientData;
            this.connect = connect;
            this.leftCallbacks = leftCallbacks;
        }

        @Override
        public void onSuccess(@Nullable PluginOnConnectResult result) {
            if (result == null) {
                LOGGER.error("OnConnectCallback finished without result, not authorizing");
                onFailed();
                return;
            }
            this.ctx.pipeline().fireUserEventTriggered(
                    new PluginOnConnectCallbackCompleted(this.clientData, this.connect,
                            result.isRefused(), result.getReturnCode(), this.leftCallbacks));
        }

        public void onFailure(Throwable t) {
            LOGGER.error("OnConnectCallback failed", t);
            onFailed();
        }

        private void onFailed() {
            this.ctx.pipeline().fireUserEventTriggered(
                    new PluginOnConnectCallbackCompleted(this.clientData, this.connect, true,
                            ReturnCode.REFUSED_NOT_AUTHORIZED, this.leftCallbacks));
        }
    }

    @VisibleForTesting
    static class Task implements CallableTask<PluginOnConnectResult> {
        private final OnConnectCallback callback;
        private final ClientData clientData;
        private final Connect connect;
        private final Metrics metrics;

        public Task(OnConnectCallback callback,
                    ClientData clientData,
                    Connect connect,
                    Metrics metrics) {
            this.callback = callback;
            this.clientData = clientData;
            this.connect = connect;
            this.metrics = metrics;
        }

        @Override
        public PluginOnConnectResult call() throws Exception {
            Timer.Context timer = this.metrics.pluginTimerConnect().time();
            try {
                this.callback.onConnect(this.connect, this.clientData);
                timer.stop();
                return new PluginOnConnectResult(false, null);
            } catch (RefusedConnectionException e) {
                LOGGER.debug("An exception was raised when calling the OnConnectCallback {}:", this.callback.getClass(), e);
                timer.stop();
                return new PluginOnConnectResult(true, e.getReturnCode());
            } catch (Throwable t) {
                LOGGER.error("Unhandled Exception in OnConnectCallback {}. Skipping all other handlers", this.callback.getClass());
                timer.stop();
                PluginExceptionUtils.log(t);
            }
            return new PluginOnConnectResult(true, ReturnCode.REFUSED_NOT_AUTHORIZED);
        }

        @NotNull
        public Class callbackType() {
            return this.callback.getClass();
        }
    }

    public static class PluginOnConnectResult {
        private final boolean refused;
        private final ReturnCode returnCode;

        public PluginOnConnectResult(boolean refused, ReturnCode returnCode) {
            this.refused = refused;
            this.returnCode = returnCode;
        }

        public boolean isRefused() {
            return refused;
        }

        public ReturnCode getReturnCode() {
            return returnCode;
        }
    }
}
