package ch;

import am1.Metrics;
import av.HiveMQConfigurationService;
import av.Internals;
import br1.PluginOnAuthentication;
import br1.PluginOnAuthenticationCallbackCompleted;
import br1.PluginOnAuthenticationCompleted;
import ca.CallableTask;
import ca.CallbackExecutor;
import cb1.PluginExceptionUtils;
import com.codahale.metrics.Timer;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.hivemq.spi.annotations.NotNull;
import com.hivemq.spi.callback.exception.AuthenticationException;
import com.hivemq.spi.callback.registry.CallbackRegistry;
import com.hivemq.spi.callback.security.OnAuthenticationCallback;
import com.hivemq.spi.message.Connect;
import com.hivemq.spi.message.ReturnCode;
import com.hivemq.spi.security.ClientCredentials;
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
import java.util.Deque;
import java.util.List;
import java.util.Queue;

@Singleton
@ChannelHandler.Sharable
public class PluginOnAuthenticationCallbackHandler extends ChannelInboundHandlerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(PluginOnAuthenticationCallbackHandler.class);
    private final CallbackRegistry callbackRegistry;
    private final HiveMQConfigurationService hiveMQConfigurationService;
    private final Metrics metrics;
    private final CallbackExecutor callbackExecutor;

    @Inject
    public PluginOnAuthenticationCallbackHandler(CallbackRegistry callbackRegistry,
                                                 HiveMQConfigurationService hiveMQConfigurationService,
                                                 Metrics metrics,
                                                 CallbackExecutor callbackExecutor) {
        this.callbackRegistry = callbackRegistry;
        this.hiveMQConfigurationService = hiveMQConfigurationService;
        this.metrics = metrics;
        this.callbackExecutor = callbackExecutor;
    }

    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof PluginOnAuthentication) {
            onAuthentication(ctx, (PluginOnAuthentication) evt);
        } else if (evt instanceof PluginOnAuthenticationCallbackCompleted) {
            onAuthenticationCallbackCompleted(ctx, (PluginOnAuthenticationCallbackCompleted) evt);
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    private void onAuthentication(ChannelHandlerContext ctx, PluginOnAuthentication event) {
        boolean available = this.callbackRegistry.isAvailable(OnAuthenticationCallback.class);
        if (available) {
            Deque<OnAuthenticationCallback> leftCallbacks = new ArrayDeque(this.callbackRegistry.getCallbacks(OnAuthenticationCallback.class));
            ClientCredentials clientCredentials = event.getClientCredentials();
            int expectedResultCount = leftCallbacks.size();
            OnAuthenticationCallback callback = leftCallbacks.poll();
            List<PluginOnAuthenticationResult> results = new ArrayList(leftCallbacks.size());
            submitTask(ctx, callback, clientCredentials, event.getConnect(), leftCallbacks, results, expectedResultCount);
        } else if (needAllPluginsToReturnTrue()) {
            ctx.pipeline().fireUserEventTriggered(new PluginOnAuthenticationCompleted(
                    event.getConnect(), event.getClientCredentials(), ReturnCode.REFUSED_NOT_AUTHORIZED, new AuthenticationException("No OnAuthenticationCallback available", ReturnCode.REFUSED_NOT_AUTHORIZED)));
        } else {
            ctx.pipeline().fireUserEventTriggered(new PluginOnAuthenticationCompleted(
                    event.getConnect(), event.getClientCredentials(), ReturnCode.ACCEPTED));
        }
    }

    private void onAuthenticationCallbackCompleted(ChannelHandlerContext ctx,
                                                   PluginOnAuthenticationCallbackCompleted event) {
        List<PluginOnAuthenticationResult> results = event.getResults();
        PluginOnAuthenticationResult lastResult = results.get(results.size() - 1);
        Connect connect = event.getConnect();
        ClientCredentials clientCredentials = event.getClientCredentials();
        if (lastResult.isRefused() ||
                lastResult.isAuthenticated() && !needAllPluginsToReturnTrue() ||
                !lastResult.isAuthenticated() && needAllPluginsToReturnTrue()) {
            ctx.pipeline().fireUserEventTriggered(new PluginOnAuthenticationCompleted(
                    connect, clientCredentials, lastResult.getReturnCode(), lastResult.getException()));
            if (ctx.pipeline().get(getClass()) != null) {
                ctx.pipeline().remove(this);
            }
            return;
        }
        if (results.size() == event.getExpectedResultCount()) {
            if (accepted(results)) {
                ctx.pipeline().fireUserEventTriggered(new PluginOnAuthenticationCompleted(
                        connect, clientCredentials, ReturnCode.ACCEPTED));
            } else {
                ctx.pipeline().fireUserEventTriggered(new PluginOnAuthenticationCompleted(
                        connect, clientCredentials, ReturnCode.REFUSED_NOT_AUTHORIZED));
            }
            if (ctx.pipeline().get(getClass()) != null) {
                ctx.pipeline().remove(this);
            }
            return;
        }
        Queue<OnAuthenticationCallback> leftCallbacks = event.getLeftCallbacks();
        OnAuthenticationCallback callback = leftCallbacks.poll();
        submitTask(ctx, callback, clientCredentials, connect,
                leftCallbacks, results, event.getExpectedResultCount());
    }

    private void submitTask(ChannelHandlerContext ctx,
                            OnAuthenticationCallback callback,
                            ClientCredentials clientCredentials,
                            Connect connect,
                            Queue<OnAuthenticationCallback> leftCallbacks,
                            List<PluginOnAuthenticationResult> results,
                            int expectedResultCount) {
        ListenableFuture future = this.callbackExecutor.submit(createTask(callback, clientCredentials));
        ResultCallback resultCallback = createResultCallback(ctx, clientCredentials, connect, leftCallbacks, results, expectedResultCount);
        Futures.addCallback(future, resultCallback, ctx.executor().parent());
    }


    @NotNull
    @VisibleForTesting
    Task createTask(OnAuthenticationCallback callback,
                    ClientCredentials clientCredentials) {
        return new Task(callback, clientCredentials, this.metrics);
    }

    @NotNull
    @VisibleForTesting
    ResultCallback createResultCallback(ChannelHandlerContext ctx,
                                        ClientCredentials clientCredentials,
                                        Connect connect,
                                        Queue<OnAuthenticationCallback> leftCallbacks,
                                        List<PluginOnAuthenticationResult> results,
                                        int expectedResultCount) {
        return new ResultCallback(ctx, clientCredentials, connect,
                leftCallbacks, results, expectedResultCount);
    }

    private boolean needAllPluginsToReturnTrue() {
        return this.hiveMQConfigurationService.internalConfiguration()
                .getBoolean(Internals.PLUGIN_AUTHENTICATION_NEED_ALL_PLUGINS_TO_RETURN_TRUE);
    }

    private boolean accepted(List<PluginOnAuthenticationResult> results) {
        boolean needAllPluginsToReturnTrue = needAllPluginsToReturnTrue();
        for (PluginOnAuthenticationResult result : results) {
            if (!needAllPluginsToReturnTrue && result.isAuthenticated()) {
                return true;
            }
            if (needAllPluginsToReturnTrue && !result.isAuthenticated()) {
                return false;
            }
        }
        return needAllPluginsToReturnTrue;
    }

    @VisibleForTesting
    static class ResultCallback implements FutureCallback<PluginOnAuthenticationResult> {
        private final ChannelHandlerContext ctx;
        private final ClientCredentials clientCredentials;
        private final Connect connect;
        private final Queue<OnAuthenticationCallback> leftCallbacks;
        private final List<PluginOnAuthenticationResult> results;
        private final int expectedResultCount;

        public ResultCallback(ChannelHandlerContext ctx,
                              ClientCredentials clientCredentials,
                              Connect connect,
                              Queue<OnAuthenticationCallback> leftCallbacks,
                              List<PluginOnAuthenticationResult> results,
                              int expectedResultCount) {
            this.ctx = ctx;
            this.clientCredentials = clientCredentials;
            this.connect = connect;
            this.leftCallbacks = leftCallbacks;
            this.results = results;
            this.expectedResultCount = expectedResultCount;
        }

        @Override
        public void onSuccess(@Nullable PluginOnAuthenticationResult result) {
            this.results.add(result);
            this.ctx.pipeline().fireUserEventTriggered(
                    new PluginOnAuthenticationCallbackCompleted(this.leftCallbacks, this.results, this.connect,
                            this.clientCredentials, this.expectedResultCount));
        }

        public void onFailure(Throwable t) {
            LOGGER.error("OnAuthenticationCallback failed. Skipping all other handlers");
            this.results.add(new PluginOnAuthenticationResult(false, ReturnCode.REFUSED_NOT_AUTHORIZED, true,
                    new AuthenticationException(t.getMessage() + " See log for more information", ReturnCode.REFUSED_NOT_AUTHORIZED)));
            this.ctx.pipeline().fireUserEventTriggered(
                    new PluginOnAuthenticationCallbackCompleted(this.leftCallbacks, this.results,
                            this.connect, this.clientCredentials, this.expectedResultCount));
        }
    }

    @VisibleForTesting
    static class Task implements CallableTask<PluginOnAuthenticationResult> {
        private final OnAuthenticationCallback callback;
        private final ClientCredentials clientCredentials;
        private final Metrics metrics;

        public Task(@NotNull OnAuthenticationCallback callback,
                    ClientCredentials clientCredentials,
                    Metrics metrics) {
            this.callback = callback;
            this.clientCredentials = clientCredentials;
            this.metrics = metrics;
        }

        @Override
        public PluginOnAuthenticationResult call() throws Exception {
            Timer.Context timer = this.metrics.pluginTimerAuthentication().time();
            try {
                Boolean authenticated = this.callback.checkCredentials(this.clientCredentials);
                PluginOnAuthenticationResult result = new PluginOnAuthenticationResult(authenticated, authenticated ? ReturnCode.ACCEPTED : ReturnCode.REFUSED_NOT_AUTHORIZED, false);
                timer.stop();
                return result;
            } catch (AuthenticationException e) {
                LOGGER.debug("An exception was raised when calling the OnAuthenticationCallback {}:", this.callback.getClass(), e);
                PluginOnAuthenticationResult result = new PluginOnAuthenticationResult(false, e.getReturnCode(), true, e);
                timer.stop();
                return result;
            } catch (Throwable t) {
                LOGGER.error("Unhandled Exception in OnAuthenticationCallback {}. Skipping all other handlers", this.callback.getClass());
                timer.stop();
                PluginExceptionUtils.log(t);
                return new PluginOnAuthenticationResult(false, ReturnCode.REFUSED_NOT_AUTHORIZED, true,
                        new AuthenticationException(t.getMessage() + " See log for more information", ReturnCode.REFUSED_NOT_AUTHORIZED));
            }
        }

        @NotNull
        public Class callbackType() {
            return this.callback.getClass();
        }
    }
}
