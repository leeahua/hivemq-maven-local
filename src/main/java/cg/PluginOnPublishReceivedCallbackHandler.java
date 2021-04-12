package cg;

import am1.Metrics;
import bu.InternalPublish;
import bw1.PluginOnPublishReceived;
import bw1.PluginOnPublishReceivedCallbackCompleted;
import bw1.PluginOnPublishReceivedCompleted;
import ca.CallableTask;
import ca.CallbackExecutor;
import cb1.PluginExceptionUtils;
import com.codahale.metrics.Timer;
import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.hivemq.spi.annotations.NotNull;
import com.hivemq.spi.callback.events.OnPublishReceivedCallback;
import com.hivemq.spi.callback.exception.OnPublishReceivedException;
import com.hivemq.spi.callback.registry.CallbackRegistry;
import com.hivemq.spi.security.ClientData;
import d.CacheScoped;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.concurrent.EventExecutorGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Queue;

@CacheScoped
public class PluginOnPublishReceivedCallbackHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(PluginOnPublishReceivedCallbackHandler.class);
    private final CallbackRegistry callbackRegistry;
    private final Metrics metrics;
    private final EventBus eventBus;
    private final CallbackExecutor callbackExecutor;
    private final EventExecutorGroup eventExecutorGroup;

    @Inject
    PluginOnPublishReceivedCallbackHandler(CallbackRegistry callbackRegistry,
                                           Metrics metrics,
                                           EventBus eventBus,
                                           CallbackExecutor callbackExecutor,
                                           EventExecutorGroup eventExecutorGroup) {
        this.callbackRegistry = callbackRegistry;
        this.metrics = metrics;
        this.eventBus = eventBus;
        this.callbackExecutor = callbackExecutor;
        this.eventExecutorGroup = eventExecutorGroup;
        eventBus.register(this);
    }

    @Subscribe
    @AllowConcurrentEvents
    public void onEvent(PluginOnPublishReceived event) {
        boolean available = this.callbackRegistry.isAvailable(OnPublishReceivedCallback.class);
        if (!available) {
            this.eventBus.post(new PluginOnPublishReceivedCompleted(event.getPublish(), event.getClientData()));
            return;
        }
        Deque<OnPublishReceivedCallback> leftCallbacks = new ArrayDeque(this.callbackRegistry.getCallbacks(OnPublishReceivedCallback.class));
        ClientData clientData = event.getClientData();
        InternalPublish publish = event.getPublish();
        OnPublishReceivedCallback callback = leftCallbacks.poll();
        submitTask(event.getCtx(), leftCallbacks, callback, publish, clientData);
    }

    @Subscribe
    @AllowConcurrentEvents
    public void onEvent(PluginOnPublishReceivedCallbackCompleted event) {
        Queue<OnPublishReceivedCallback> leftCallbacks = event.getLeftCallbacks();
        OnPublishReceivedCallback callback = leftCallbacks.poll();
        InternalPublish publish = event.getPublish();
        ClientData clientData = event.getClientData();
        submitTask(event.getCtx(), leftCallbacks, callback, publish, clientData);
    }

    private void submitTask(ChannelHandlerContext ctx,
                            Queue<OnPublishReceivedCallback> leftCallbacks,
                            OnPublishReceivedCallback callback,
                            InternalPublish publish,
                            ClientData clientData) {
        ListenableFuture<Result> future = this.callbackExecutor.submit(
                new Task(ctx, callback, publish, clientData, this.metrics));
        Futures.addCallback(future,
                new ResultCallback(ctx, this.eventBus, leftCallbacks, publish, clientData),
                this.eventExecutorGroup);
    }

    static final class ResultCallback implements FutureCallback<Result> {
        private static final Logger LOGGER = LoggerFactory.getLogger(ResultCallback.class);
        private final ChannelHandlerContext ctx;
        private final EventBus eventBus;
        private final Queue<OnPublishReceivedCallback> leftCallbacks;
        private final InternalPublish publish;
        private final ClientData clientData;

        public ResultCallback(ChannelHandlerContext ctx,
                              EventBus eventBus,
                              Queue<OnPublishReceivedCallback> leftCallbacks,
                              InternalPublish publish,
                              ClientData clientData) {
            this.ctx = ctx;
            this.eventBus = eventBus;
            this.leftCallbacks = leftCallbacks;
            this.publish = publish;
            this.clientData = clientData;
        }

        @Override
        public void onSuccess(@Nullable Result result) {
            if (!result.isSuccess()) {
                return;
            }
            if (this.leftCallbacks.isEmpty()) {
                this.eventBus.post(new PluginOnPublishReceivedCompleted(this.publish, this.clientData));
                return;
            }
            this.eventBus.post(new PluginOnPublishReceivedCallbackCompleted(this.ctx, this.leftCallbacks, this.clientData, this.publish));
        }

        public void onFailure(Throwable t) {
            LOGGER.warn("OnPublishReceivedCallback failed", t);
            if (this.leftCallbacks.isEmpty()) {
                this.eventBus.post(new PluginOnPublishReceivedCompleted(this.publish, this.clientData));
                return;
            }
            this.eventBus.post(new PluginOnPublishReceivedCallbackCompleted(this.ctx, this.leftCallbacks, this.clientData, this.publish));
        }
    }

    static class Task implements CallableTask<Result> {
        private final ChannelHandlerContext ctx;
        private final OnPublishReceivedCallback callback;
        private final InternalPublish publish;
        private final ClientData clientData;
        private final Metrics metrics;

        public Task(ChannelHandlerContext ctx,
                    OnPublishReceivedCallback callback,
                    InternalPublish publish,
                    ClientData clientData,
                    Metrics metrics) {
            this.ctx = ctx;
            this.callback = callback;
            this.publish = publish;
            this.clientData = clientData;
            this.metrics = metrics;
        }

        @Override
        public Result call() throws Exception {
            Timer.Context timer = this.metrics.pluginTimerPublishReceived().time();
            try {
                this.callback.onPublishReceived(this.publish, this.clientData);
                timer.stop();
                return new Result(true);
            } catch (OnPublishReceivedException e) {
                LOGGER.debug("An exception was raised when calling the OnPublishReceivedCallback {}.\n {}",
                        this.callback.getClass(), e.getMessage());
                if (e.getDisconnectClient()) {
                    this.ctx.channel().close();
                }
                timer.stop();
                return new Result(false);
            } catch (Throwable e) {
                LOGGER.error("Unhandled Exception in onPublishReceivedCallback {}. Skipping all other handlers",
                        this.callback.getClass());
                timer.stop();
                PluginExceptionUtils.log(e);
            }
            return new Result(false);
        }

        @NotNull
        public Class callbackType() {
            return this.callback.getClass();
        }
    }

    static class Result {
        private final boolean success;

        Result(boolean success) {
            this.success = success;
        }

        public boolean isSuccess() {
            return success;
        }
    }
}
