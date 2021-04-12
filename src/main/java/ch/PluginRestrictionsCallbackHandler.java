package ch;

import am1.Metrics;
import br1.PluginRestrictionsAfterLogin;
import br1.PluginRestrictionsAfterLoginCallbackCompleted;
import br1.PluginRestrictionsAfterLoginCompleted;
import ca.CallableTask;
import ca.CallbackExecutor;
import cb1.PluginExceptionUtils;
import com.codahale.metrics.Timer;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.hivemq.spi.annotations.NotNull;
import com.hivemq.spi.callback.registry.CallbackRegistry;
import com.hivemq.spi.callback.security.RestrictionsAfterLoginCallback;
import com.hivemq.spi.message.Connect;
import com.hivemq.spi.security.ClientCredentials;
import com.hivemq.spi.security.Restriction;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Set;

public class PluginRestrictionsCallbackHandler extends ChannelInboundHandlerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(PluginRestrictionsCallbackHandler.class);
    private final CallbackRegistry callbackRegistry;
    private final List<Set<Restriction>> restrictions;
    private final Metrics metrics;
    private final CallbackExecutor callbackExecutor;
    private int expectedResultCount = 0;

    public PluginRestrictionsCallbackHandler(CallbackRegistry callbackRegistry,
                                             Metrics metrics,
                                             CallbackExecutor callbackExecutor) {
        this.callbackRegistry = callbackRegistry;
        this.metrics = metrics;
        this.callbackExecutor = callbackExecutor;
        this.restrictions = Lists.newArrayList();
    }

    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof PluginRestrictionsAfterLogin) {
            restrictionsAfterLogin(ctx, (PluginRestrictionsAfterLogin) evt);
        } else if (evt instanceof PluginRestrictionsAfterLoginCallbackCompleted) {
            restrictionsAfterLoginCallbackCompleted(ctx, (PluginRestrictionsAfterLoginCallbackCompleted) evt);
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    private void restrictionsAfterLogin(ChannelHandlerContext ctx,
                                        PluginRestrictionsAfterLogin event) {
        boolean available = this.callbackRegistry.isAvailable(RestrictionsAfterLoginCallback.class);
        if (available) {
            Deque<RestrictionsAfterLoginCallback> callbacks = new ArrayDeque<>(this.callbackRegistry.getCallbacks(RestrictionsAfterLoginCallback.class));
            this.expectedResultCount = callbacks.size();
            ClientCredentials clientCredentials = event.getClientCredentials();
            callbacks.forEach(callback ->
                    submitTask(ctx, callback, clientCredentials, event.getConnect()));
        } else {
            ctx.pipeline().fireUserEventTriggered(new PluginRestrictionsAfterLoginCompleted(event.getConnect(), event.getClientCredentials(), Sets.newHashSet()));
        }
    }

    private void restrictionsAfterLoginCallbackCompleted(
            ChannelHandlerContext ctx,
            PluginRestrictionsAfterLoginCallbackCompleted event) {
        Set<Restriction> restrictions = event.getRestrictions();
        Connect connect = event.getConnect();
        ClientCredentials clientCredentials = event.getClientCredentials();
        this.restrictions.add(restrictions);
        if (this.restrictions.size() == this.expectedResultCount) {
            Iterable restrictionIterable = Iterables.concat(this.restrictions);
            ctx.pipeline().fireUserEventTriggered(
                    new PluginRestrictionsAfterLoginCompleted(connect, clientCredentials, restrictionIterable));
            ctx.pipeline().remove(this);
        }
    }

    private void submitTask(ChannelHandlerContext ctx,
                            RestrictionsAfterLoginCallback callback,
                            ClientCredentials clientCredentials,
                            Connect connect) {
        ListenableFuture future = this.callbackExecutor.submit(
                createTask(ctx, callback, clientCredentials));
        Futures.addCallback(future,
                createResultCallback(ctx, clientCredentials, connect),
                ctx.executor().parent());
    }

    @NotNull
    @VisibleForTesting
    Task createTask(ChannelHandlerContext ctx,
                    RestrictionsAfterLoginCallback callback,
                    ClientCredentials clientCredentials) {
        return new Task(ctx, callback, clientCredentials, this.metrics);
    }

    @NotNull
    @VisibleForTesting
    ResultCallback createResultCallback(ChannelHandlerContext ctx, ClientCredentials paramClientCredentials, Connect paramConnect) {
        return new ResultCallback(ctx, paramClientCredentials, paramConnect);
    }

    @VisibleForTesting
    static class ResultCallback implements FutureCallback<Set<Restriction>> {
        private final ChannelHandlerContext ctx;
        private final ClientCredentials clientCredentials;
        private final Connect connect;

        public ResultCallback(ChannelHandlerContext ctx,
                              ClientCredentials clientCredentials,
                              Connect connect) {
            this.ctx = ctx;
            this.clientCredentials = clientCredentials;
            this.connect = connect;
        }

        @Override
        public void onSuccess(@Nullable Set<Restriction> result) {
            this.ctx.pipeline().fireUserEventTriggered(new PluginRestrictionsAfterLoginCallbackCompleted(
                    this.clientCredentials, this.connect, result));
        }

        public void onFailure(Throwable t) {
            LOGGER.error("RestrictionsAfterLoginCallback failed.", t);
            this.ctx.pipeline().fireUserEventTriggered(new PluginRestrictionsAfterLoginCallbackCompleted(
                    this.clientCredentials, this.connect, Collections.emptySet()));
        }
    }

    @VisibleForTesting
    static class Task
            implements CallableTask<Set<Restriction>> {
        private final ChannelHandlerContext ctx;
        private final RestrictionsAfterLoginCallback callback;
        private final ClientCredentials credentials;
        private final Metrics metrics;

        public Task(ChannelHandlerContext ctx,
                    RestrictionsAfterLoginCallback callback,
                    ClientCredentials clientCredentials,
                    Metrics metrics) {
            this.ctx = ctx;
            this.callback = callback;
            this.credentials = clientCredentials;
            this.metrics = metrics;
        }

        @Override
        public Set<Restriction> call() throws Exception {
            Timer.Context timer = this.metrics.pluginTimerRestrictions().time();
            try {
                Set<Restriction> restrictions = this.callback.getRestrictions(this.credentials);
                timer.stop();
                return restrictions;
            } catch (Throwable t) {
                LOGGER.error("Unhandled Exception in RestrictionsAfterLoginCallback {}.", this.callback.getClass());
                timer.stop();
                PluginExceptionUtils.log(t);
            }
            return Sets.newHashSet();
        }

        @NotNull
        public Class callbackType() {
            return this.callback.getClass();
        }
    }
}
