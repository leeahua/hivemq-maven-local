package cg;

import am1.Metrics;
import by1.PluginOnSubscribe;
import by1.PluginOnSubscribeCallbackCompleted;
import by1.PluginOnSubscribeCompleted;
import ca.CallableTask;
import ca.CallbackExecutor;
import cb1.PluginExceptionUtils;
import com.codahale.metrics.Timer;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.hivemq.spi.annotations.NotNull;
import com.hivemq.spi.callback.events.OnSubscribeCallback;
import com.hivemq.spi.callback.exception.InvalidSubscriptionException;
import com.hivemq.spi.callback.registry.CallbackRegistry;
import com.hivemq.spi.message.Subscribe;
import com.hivemq.spi.security.ClientData;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.concurrent.EventExecutorGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Queue;

@ChannelHandler.Sharable
@Singleton
public class PluginOnSubscribeCallbackHandler extends ChannelInboundHandlerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(PluginOnSubscribeCallbackHandler.class);
    private final CallbackRegistry callbackRegistry;
    private final Metrics metrics;
    private final CallbackExecutor callbackExecutor;
    private final EventExecutorGroup eventExecutorGroup;

    @Inject
    PluginOnSubscribeCallbackHandler(CallbackRegistry callbackRegistry,
                                     Metrics metrics,
                                     CallbackExecutor callbackExecutor,
                                     EventExecutorGroup eventExecutorGroup) {
        this.callbackRegistry = callbackRegistry;
        this.metrics = metrics;
        this.callbackExecutor = callbackExecutor;
        this.eventExecutorGroup = eventExecutorGroup;
    }

    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof PluginOnSubscribe) {
            onSubscribe(ctx, (PluginOnSubscribe) evt);
        } else if (evt instanceof PluginOnSubscribeCallbackCompleted) {
            onSubscribeCallbackCompleted(ctx, (PluginOnSubscribeCallbackCompleted) evt);
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    private void onSubscribe(ChannelHandlerContext ctx, PluginOnSubscribe event) {
        boolean available = this.callbackRegistry.isAvailable(OnSubscribeCallback.class);
        if (!available) {
            return;
        }
        Deque<OnSubscribeCallback> leftCallbacks = new ArrayDeque(this.callbackRegistry.getCallbacks(OnSubscribeCallback.class));
        ClientData clientData = event.getClientData();
        Subscribe subscribe = event.getSubscribe();
        OnSubscribeCallback callback = leftCallbacks.poll();
        submitTask(ctx, leftCallbacks, callback, subscribe, clientData, event.getGrantedQoSNumbers());
    }

    private void onSubscribeCallbackCompleted(ChannelHandlerContext ctx,
                                              PluginOnSubscribeCallbackCompleted event) {
        Queue<OnSubscribeCallback> leftCallbacks = event.getLeftCallbacks();
        OnSubscribeCallback callback = leftCallbacks.poll();
        Subscribe subscribe = event.getSubscribe();
        ClientData clientData = event.getClientData();
        submitTask(ctx, leftCallbacks, callback, subscribe,
                clientData, event.getGrantedQoSNumbers());
    }

    private void submitTask(ChannelHandlerContext ctx,
                            Queue<OnSubscribeCallback> leftCallbacks,
                            OnSubscribeCallback callback,
                            Subscribe subscribe,
                            ClientData clientData,
                            byte[] grantedQoSNumbers) {
        ListenableFuture<Boolean> future = this.callbackExecutor.submit(
                new Task(ctx, callback, subscribe, clientData, this.metrics));
        Futures.addCallback(future, new ResultCallback(ctx, leftCallbacks, subscribe, clientData, grantedQoSNumbers), this.eventExecutorGroup);
    }

    private static final class ResultCallback implements FutureCallback<Boolean> {
        private final ChannelHandlerContext ctx;
        private final Queue<OnSubscribeCallback> leftCallbacks;
        private final Subscribe subscribe;
        private final ClientData clientData;
        private final byte[] grantedQoSNumbers;

        public ResultCallback(ChannelHandlerContext ctx,
                              Queue<OnSubscribeCallback> leftCallbacks,
                              Subscribe subscribe,
                              ClientData clientData,
                              byte[] grantedQoSNumbers) {
            this.ctx = ctx;
            this.leftCallbacks = leftCallbacks;
            this.subscribe = subscribe;
            this.clientData = clientData;
            this.grantedQoSNumbers = grantedQoSNumbers;
        }

        @Override
        public void onSuccess(@Nullable Boolean result) {
            if (result == null || !result) {
                return;
            }
            if (this.leftCallbacks.isEmpty()) {
                this.ctx.pipeline().fireUserEventTriggered(
                        new PluginOnSubscribeCompleted(this.subscribe,
                                this.grantedQoSNumbers, this.clientData));
            }
            this.ctx.pipeline().fireUserEventTriggered(
                    new PluginOnSubscribeCallbackCompleted(this.leftCallbacks, this.clientData,
                            this.subscribe, this.grantedQoSNumbers));
        }

        public void onFailure(Throwable t) {
            LOGGER.error("OnSubscribeCallback failed", t);
        }
    }

    static class Task implements CallableTask<Boolean> {
        private final ChannelHandlerContext ctx;
        private final OnSubscribeCallback callback;
        private final Subscribe subscribe;
        private final ClientData clientData;
        private final Metrics metrics;

        public Task(ChannelHandlerContext ctx,
                    OnSubscribeCallback callback,
                    Subscribe subscribe,
                    ClientData clientData,
                    Metrics metrics) {
            this.ctx = ctx;
            this.callback = callback;
            this.subscribe = subscribe;
            this.clientData = clientData;
            this.metrics = metrics;
        }

        @Override
        public Boolean call() throws Exception {
            Timer.Context timer = this.metrics.pluginTimerSubscribe().time();
            try {
                this.callback.onSubscribe(this.subscribe, this.clientData);
                timer.stop();
                return Boolean.TRUE;
            } catch (InvalidSubscriptionException e) {
                LOGGER.debug("An exception was raised when calling the OnSubscribeCallback {}:",
                        this.callback.getClass(), e);
                this.ctx.channel().close();
                timer.stop();
                return Boolean.FALSE;
            } catch (Throwable e) {
                LOGGER.error("Unhandled Exception in onSubscribeCallback {}. Skipping all other handlers",
                        this.callback.getClass());
                timer.stop();
                PluginExceptionUtils.log(e);
            }
            return Boolean.FALSE;
        }

        @NotNull
        public Class callbackType() {
            return this.callback.getClass();
        }
    }
}
