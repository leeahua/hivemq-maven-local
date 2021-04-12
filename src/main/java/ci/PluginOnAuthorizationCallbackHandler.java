package ci;

import am1.Metrics;
import bs1.AuthorizationType;
import bs1.PluginOnAuthorization;
import bs1.PluginOnAuthorizationCallbackCompleted;
import bs1.PluginOnAuthorizationCompleted;
import bu.InternalPublish;
import ca.CallableTask;
import ca.CallbackExecutor;
import cb1.PluginExceptionUtils;
import com.codahale.metrics.Timer;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.hivemq.spi.annotations.NotNull;
import com.hivemq.spi.callback.exception.AuthenticationException;
import com.hivemq.spi.callback.registry.CallbackRegistry;
import com.hivemq.spi.callback.security.OnAuthorizationCallback;
import com.hivemq.spi.callback.security.authorization.AuthorizationBehaviour;
import com.hivemq.spi.message.Connect;
import com.hivemq.spi.message.ReturnCode;
import com.hivemq.spi.message.Subscribe;
import com.hivemq.spi.security.AuthorizationEvaluator;
import com.hivemq.spi.security.ClientData;
import com.hivemq.spi.topic.MqttTopicPermission;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Queue;
import java.util.stream.Collectors;

@Singleton
@ChannelHandler.Sharable
public class PluginOnAuthorizationCallbackHandler extends ChannelInboundHandlerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(PluginOnAuthorizationCallbackHandler.class);
    private final CallbackRegistry callbackRegistry;
    private final Metrics metrics;
    private final CallbackExecutor callbackExecutor;

    @Inject
    public PluginOnAuthorizationCallbackHandler(CallbackRegistry callbackRegistry,
                                                Metrics metrics,
                                                CallbackExecutor callbackExecutor) {
        this.callbackRegistry = callbackRegistry;
        this.metrics = metrics;
        this.callbackExecutor = callbackExecutor;
    }

    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof PluginOnAuthorization) {
            onAuthorization(ctx, (PluginOnAuthorization) evt);
        } else if (evt instanceof PluginOnAuthorizationCallbackCompleted) {
            onAuthorizationCallbackCompleted(ctx, (PluginOnAuthorizationCallbackCompleted) evt);
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    private void onAuthorization(ChannelHandlerContext ctx, PluginOnAuthorization event) {
        boolean available = this.callbackRegistry.isAvailable(OnAuthorizationCallback.class);
        if (available) {
            Deque<OnAuthorizationCallback> leftCallbacks = new ArrayDeque<>(this.callbackRegistry.getCallbacks(OnAuthorizationCallback.class));
            ClientData clientData = event.getClientData();
            OnAuthorizationCallback callback = leftCallbacks.poll();
            List<AuthorizationResultImpl> results = new ArrayList<>();
            submitTask(ctx, callback, clientData, event.getSubscribe(), event.getPublish(),
                    leftCallbacks, results, event.getType(), event.getException(),
                    event.getConnect(), event.getReturnCode(), event.isAccepted());
        } else {
            ctx.pipeline().fireUserEventTriggered(new PluginOnAuthorizationCompleted(
                    event.getSubscribe(), event.getPublish(), event.getType(), event.getClientData(), AuthorizationBehaviour.NEXT, null, event.getException(), event.getConnect(), event.getReturnCode(), event.isAccepted()));
        }
    }

    private void onAuthorizationCallbackCompleted(ChannelHandlerContext ctx,
                                                  PluginOnAuthorizationCallbackCompleted event) {
        List<AuthorizationResultImpl> results = event.getResults();
        Subscribe subscribe = event.getSubscribe();
        InternalPublish publish = event.getPublish();
        ClientData clientData = event.getClientData();
        Queue<OnAuthorizationCallback> leftCallbacks = event.getLeftCallbacks();
        boolean needContinue;
        if (publish != null) {
            needContinue = checkPublishAuthorization(ctx, publish, results, clientData,
                    leftCallbacks, event.getType(), event.getException(),
                    event.getConnect(), event.getReturnCode(), event.isAccepted());
        } else if (subscribe != null) {
            needContinue = checkSubscribeAuthorization(ctx, subscribe, results, clientData,
                    leftCallbacks, event.getType());
        } else {
            throw new IllegalStateException("Neither publish nor subscribe are set for AuthorizationCallback");
        }
        if (!needContinue) {
            return;
        }
        OnAuthorizationCallback callback = leftCallbacks.poll();
        submitTask(ctx, callback, clientData, subscribe, publish, leftCallbacks,
                results, event.getType(), event.getException(), event.getConnect(),
                event.getReturnCode(), event.isAccepted());
    }

    private boolean checkPublishAuthorization(ChannelHandlerContext ctx,
                                              InternalPublish publish,
                                              List<AuthorizationResultImpl> results,
                                              ClientData clientData,
                                              Queue<OnAuthorizationCallback> leftCallabcks,
                                              AuthorizationType type,
                                              AuthenticationException exception,
                                              Connect connect,
                                              ReturnCode returnCode,
                                              boolean accepted) {
        AuthorizationResultImpl lastResult = results.get(results.size() - 1);
        AuthorizationBehaviour behaviour = AuthorizationEvaluator.checkPublish(
                publish.getTopic(), publish.getQoS(), publish.isRetain(), lastResult);
        if (behaviour != AuthorizationBehaviour.NEXT) {
            ctx.pipeline().fireUserEventTriggered(new PluginOnAuthorizationCompleted(
                    null, publish, type, clientData, behaviour, null,
                    exception, connect, returnCode, accepted));
            return false;
        }
        if (leftCallabcks.size() == 0) {
            ctx.pipeline().fireUserEventTriggered(new PluginOnAuthorizationCompleted(
                    null, publish, type, clientData, AuthorizationBehaviour.NEXT, null,
                    exception, connect, returnCode, accepted));
            return false;
        }
        return true;
    }

    private boolean checkSubscribeAuthorization(ChannelHandlerContext ctx,
                                                Subscribe subscribe,
                                                List<AuthorizationResultImpl> results,
                                                ClientData clientData,
                                                Queue<OnAuthorizationCallback> leftCallbacks,
                                                AuthorizationType type) {
        List<AuthorizationBehaviour> behaviours = Lists.newArrayList();
        subscribe.getTopics().forEach(topic -> {
            List<AuthorizationBehaviour> topicBehaviours = results.stream().map(result ->
                    AuthorizationEvaluator.checkSubscription(topic.getTopic(), topic.getQoS(), result))
                    .filter(behaviour -> behaviour != AuthorizationBehaviour.NEXT)
                    .collect(Collectors.toList());
            behaviours.addAll(topicBehaviours);
            if (topicBehaviours.isEmpty()) {
                behaviours.add(AuthorizationBehaviour.NEXT);
            }
        });
        if (!behaviours.contains(AuthorizationBehaviour.NEXT) || leftCallbacks.size() == 0) {
            ctx.pipeline().fireUserEventTriggered(new PluginOnAuthorizationCompleted(
                    subscribe, null, type, clientData, null, behaviours,
                    null, null, null, true));
            return false;
        }
        return true;
    }

    private void submitTask(ChannelHandlerContext ctx,
                            OnAuthorizationCallback callback,
                            ClientData clientData,
                            Subscribe subscribe,
                            InternalPublish publish,
                            Queue<OnAuthorizationCallback> leftCallbacks,
                            List<AuthorizationResultImpl> results,
                            AuthorizationType type,
                            AuthenticationException exception,
                            Connect connect,
                            ReturnCode returnCode,
                            boolean accepted) {
        ListenableFuture future = this.callbackExecutor.submit(createTask(callback, clientData));
        Futures.addCallback(future,
                createResultCallback(ctx, clientData, subscribe, publish, leftCallbacks,
                        results, type, exception, connect, returnCode, accepted),
                ctx.executor().parent());
    }

    @NotNull
    @VisibleForTesting
    Task createTask(OnAuthorizationCallback callback, ClientData clientData) {
        return new Task(callback, clientData, this.metrics);
    }

    @NotNull
    @VisibleForTesting
    ResultCallback createResultCallback(ChannelHandlerContext ctx,
                                        ClientData clientData,
                                        Subscribe subscribe,
                                        InternalPublish publish,
                                        Queue<OnAuthorizationCallback> leftCallbacks,
                                        List<AuthorizationResultImpl> results,
                                        AuthorizationType type) {
        return createResultCallback(ctx, clientData, subscribe, publish, leftCallbacks,
                results, type, null, null, null, true);
    }

    @NotNull
    @VisibleForTesting
    ResultCallback createResultCallback(ChannelHandlerContext ctx,
                                        ClientData clientData,
                                        Subscribe subscribe,
                                        InternalPublish publish,
                                        Queue<OnAuthorizationCallback> leftCallbacks,
                                        List<AuthorizationResultImpl> results,
                                        AuthorizationType type,
                                        AuthenticationException exception,
                                        Connect connect,
                                        ReturnCode returnCode,
                                        boolean accepted) {
        return new ResultCallback(ctx, clientData, subscribe, publish, leftCallbacks, results,
                type, exception, connect, returnCode, accepted);
    }

    @VisibleForTesting
    static class ResultCallback
            implements FutureCallback<AuthorizationResultImpl> {
        private final ChannelHandlerContext ctx;
        private final ClientData clientData;
        private final Subscribe subscribe;
        private final InternalPublish publish;
        private final Queue<OnAuthorizationCallback> leftCallbacks;
        private final List<AuthorizationResultImpl> results;
        private final AuthorizationType type;
        private final AuthenticationException exception;
        private final Connect connect;
        private final ReturnCode returnCode;
        private final boolean accepted;

        public ResultCallback(ChannelHandlerContext ctx,
                              ClientData clientData,
                              Subscribe subscribe,
                              InternalPublish publish,
                              Queue<OnAuthorizationCallback> leftCallbacks,
                              List<AuthorizationResultImpl> results,
                              AuthorizationType type,
                              AuthenticationException exception,
                              Connect connect,
                              ReturnCode returnCode,
                              boolean accepted) {
            this.ctx = ctx;
            this.clientData = clientData;
            this.subscribe = subscribe;
            this.leftCallbacks = leftCallbacks;
            this.results = results;
            this.publish = publish;
            this.type = type;
            this.exception = exception;
            this.connect = connect;
            this.returnCode = returnCode;
            this.accepted = accepted;
        }

        @Override
        public void onSuccess(@Nullable AuthorizationResultImpl result) {
            this.results.add(result);
            fireCallbackCompleted();
        }

        public void onFailure(Throwable t) {
            LOGGER.error("AuthorizationCallback failed.", t);
            this.results.add(new AuthorizationResultImpl(Collections.emptyList(), AuthorizationBehaviour.DENY));
            fireCallbackCompleted();
        }

        private void fireCallbackCompleted() {
            this.ctx.pipeline().fireUserEventTriggered(
                    new PluginOnAuthorizationCallbackCompleted(this.leftCallbacks, this.results, this.subscribe,
                            this.publish, this.clientData, this.type, this.exception,
                            this.connect, this.returnCode, this.accepted));
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
            } catch (Throwable t) {
                LOGGER.error("Unhandled Exception in OnAuthorizationCallback.getDefaultBehaviour {} using NEXT and skipping this callback.",
                        this.callback.getClass());
                timer.stop();
                PluginExceptionUtils.log(t);
                return new AuthorizationResultImpl(Lists.newArrayList(), AuthorizationBehaviour.NEXT);
            }
            try {
                List<MqttTopicPermission> permissions = this.callback.getPermissionsForClient(this.clientData);
                timer.stop();
                return new AuthorizationResultImpl(permissions, behaviour);
            } catch (Throwable t) {
                LOGGER.error("Unhandled Exception in OnAuthorizationCallback {}.", this.callback.getClass());
                timer.stop();
                PluginExceptionUtils.log(t);
            }
            return new AuthorizationResultImpl(Lists.newArrayList(), behaviour);
        }

        @NotNull
        public Class callbackType() {
            return this.callback.getClass();
        }
    }
}
