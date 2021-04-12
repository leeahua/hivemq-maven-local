package ci;

import am1.Metrics;
import bs1.PluginOnAuthorizationPublish;
import bs1.PluginOnAuthorizationPublishCallbackCompleted;
import bs1.PluginOnAuthorizationPublishCompleted;
import bu.InternalPublish;
import ca.CallableTask;
import ca.CallbackExecutor;
import cb1.PluginExceptionUtils;
import com.codahale.metrics.Timer;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.hivemq.spi.annotations.NotNull;
import com.hivemq.spi.callback.exception.AuthenticationException;
import com.hivemq.spi.callback.registry.CallbackRegistry;
import com.hivemq.spi.callback.security.OnAuthorizationCallback;
import com.hivemq.spi.callback.security.authorization.AuthorizationBehaviour;
import com.hivemq.spi.security.AuthorizationEvaluator;
import com.hivemq.spi.security.ClientData;
import com.hivemq.spi.topic.MqttTopicPermission;
import d.CacheScoped;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.concurrent.EventExecutorGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Queue;

@CacheScoped
public class PluginOnAuthorizationPublishCallbackHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(PluginOnAuthorizationPublishCallbackHandler.class);
    private final CallbackRegistry callbackRegistry;
    private final Metrics metrics;
    private final EventBus eventBus;
    private final CallbackExecutor callbackExecutor;
    private final EventExecutorGroup eventExecutorGroup;

    @Inject
    public PluginOnAuthorizationPublishCallbackHandler(CallbackRegistry callbackRegistry,
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
    public void onEvent(PluginOnAuthorizationPublish event) {
        boolean available = this.callbackRegistry.isAvailable(OnAuthorizationCallback.class);
        if (!available) {
            this.eventBus.post(new PluginOnAuthorizationPublishCompleted(
                    event.getCtx(), event.getPublish(), event.getClientData(),
                    AuthorizationBehaviour.NEXT, event.getException()));
            return;
        }
        Deque<OnAuthorizationCallback> leftCallbacks = new ArrayDeque<>(this.callbackRegistry.getCallbacks(OnAuthorizationCallback.class));
        ClientData clientData = event.getClientData();
        OnAuthorizationCallback callback = leftCallbacks.poll();
        List<AuthorizationResultImpl> results = new ArrayList<>();
        submitTask(event.getCtx(), this.eventBus, callback, clientData, event.getPublish(),
                leftCallbacks, results, event.getException());
    }

    @Subscribe
    @AllowConcurrentEvents
    public void onEvent(PluginOnAuthorizationPublishCallbackCompleted event) {
        List<AuthorizationResultImpl> results = event.getResults();
        InternalPublish publish = event.getPublish();
        ClientData clientData = event.getClientData();
        Queue<OnAuthorizationCallback> leftCallbacks = event.getLeftCallbacks();
        if (publish == null) {
            throw new IllegalStateException("publish cannot be null for AuthorizationCallback");
        }
        boolean needNext = needNext(event.getCtx(), this.eventBus, publish, results,
                clientData, leftCallbacks, event.getException());
        if (!needNext) {
            return;
        }
        OnAuthorizationCallback callback = leftCallbacks.poll();
        submitTask(event.getCtx(), this.eventBus, callback, clientData, publish,
                leftCallbacks, results, event.getException());
    }

    private boolean needNext(ChannelHandlerContext ctx,
                             EventBus eventBus,
                             InternalPublish publish,
                             List<AuthorizationResultImpl> results,
                             ClientData clientData,
                             Queue<OnAuthorizationCallback> leftCallbacks,
                             AuthenticationException exception) {
        AuthorizationResultImpl result = results.get(results.size() - 1);
        AuthorizationBehaviour behaviour = AuthorizationEvaluator.checkPublish(
                publish.getTopic(), publish.getQoS(), publish.isRetain(), result);
        if (behaviour != AuthorizationBehaviour.NEXT) {
            eventBus.post(new PluginOnAuthorizationPublishCompleted(
                    ctx, publish, clientData, behaviour, exception));
            return false;
        }
        if (leftCallbacks.size() == 0) {
            eventBus.post(new PluginOnAuthorizationPublishCompleted(
                    ctx, publish, clientData, AuthorizationBehaviour.NEXT, exception));
            return false;
        }
        return true;
    }

    private void submitTask(ChannelHandlerContext ctx,
                            EventBus eventBus,
                            OnAuthorizationCallback callback,
                            ClientData clientData,
                            InternalPublish publish,
                            Queue<OnAuthorizationCallback> leftCallbacks,
                            List<AuthorizationResultImpl> results,
                            AuthenticationException exception) {
        ListenableFuture<AuthorizationResultImpl> future =
                this.callbackExecutor.submit(createTask(callback, clientData));
        Futures.addCallback(future,
                createResultCallback(ctx, eventBus, clientData, publish,
                        leftCallbacks, results, exception),
                this.eventExecutorGroup);
    }

    @NotNull
    @VisibleForTesting
    Task createTask(OnAuthorizationCallback callback,
                    ClientData clientData) {
        return new Task(callback, clientData, this.metrics);
    }

    @NotNull
    @VisibleForTesting
    ResultCallback createResultCallback(ChannelHandlerContext ctx,
                                        EventBus eventBus,
                                        ClientData clientData,
                                        InternalPublish publish,
                                        Queue<OnAuthorizationCallback> leftCallbacks,
                                        List<AuthorizationResultImpl> results,
                                        AuthenticationException exception) {
        return new ResultCallback(ctx, eventBus, clientData, publish,
                leftCallbacks, results, exception);
    }

    @VisibleForTesting
    static class ResultCallback implements FutureCallback<AuthorizationResultImpl> {
        private final ChannelHandlerContext ctx;
        private final EventBus eventBus;
        private final ClientData clientData;
        private final InternalPublish publish;
        private final Queue<OnAuthorizationCallback> leftCallbacks;
        private final List<AuthorizationResultImpl> results;
        private final AuthenticationException exception;

        public ResultCallback(ChannelHandlerContext ctx,
                              EventBus eventBus,
                              ClientData clientData,
                              InternalPublish publish,
                              Queue<OnAuthorizationCallback> leftCallbacks,
                              List<AuthorizationResultImpl> results,
                              AuthenticationException exception) {
            this.ctx = ctx;
            this.eventBus = eventBus;
            this.clientData = clientData;
            this.leftCallbacks = leftCallbacks;
            this.results = results;
            this.publish = publish;
            this.exception = exception;
        }

        @Override
        public void onSuccess(@Nullable AuthorizationResultImpl result) {
            this.results.add(result);
            this.eventBus.post(new PluginOnAuthorizationPublishCallbackCompleted(
                    this.ctx, this.leftCallbacks, this.results, this.publish, this.clientData, this.exception));
        }

        public void onFailure(Throwable t) {
            LOGGER.warn("OnAuthorizationCallback failed for Publish, authorization denied", t);
            this.results.add(new AuthorizationResultImpl(Lists.newArrayList(), AuthorizationBehaviour.DENY));
            this.eventBus.post(new PluginOnAuthorizationPublishCallbackCompleted(
                    this.ctx, this.leftCallbacks, this.results, this.publish, this.clientData, this.exception));
        }
    }

    @VisibleForTesting
    static class Task implements CallableTask<AuthorizationResultImpl> {
        private final OnAuthorizationCallback callback;
        private final ClientData clientData;
        private final Metrics metrics;

        public Task(OnAuthorizationCallback callback,
                    ClientData clientData,
                    Metrics metrics) {
            this.callback = callback;
            this.clientData = clientData;
            this.metrics = metrics;
        }

        @Override
        public AuthorizationResultImpl call() throws Exception {
            Timer.Context timer = this.metrics.pluginTimerAuthorization().time();
            AuthorizationBehaviour behaviour;
            try {
                behaviour = this.callback.getDefaultBehaviour();
            } catch (Throwable e) {
                LOGGER.error("Unhandled Exception in OnAuthorizationCallback.getDefaultBehaviour {} using NEXT and skipping this callback.",
                        this.callback.getClass());
                timer.stop();
                PluginExceptionUtils.log(e);
                return new AuthorizationResultImpl(Lists.newArrayList(), AuthorizationBehaviour.NEXT);
            }
            try {
                List<MqttTopicPermission> permissions = this.callback.getPermissionsForClient(this.clientData);
                timer.stop();
                return new AuthorizationResultImpl(permissions, behaviour);
            } catch (Throwable e) {
                LOGGER.error("Unhandled Exception in OnAuthorizationCallback {}.", this.callback.getClass());
                timer.stop();
                PluginExceptionUtils.log(e);
            }
            return new AuthorizationResultImpl(Lists.newArrayList(), behaviour);
        }

        @NotNull
        public Class callbackType() {
            return this.callback.getClass();
        }
    }
}
