package ch;

import am1.Metrics;
import br1.PluginAfterLogin;
import ca.CallbackExecutor;
import ca.RunnableTask;
import cb1.PluginExceptionUtils;
import com.codahale.metrics.Timer;
import com.google.common.annotations.VisibleForTesting;
import com.hivemq.spi.annotations.NotNull;
import com.hivemq.spi.callback.exception.AuthenticationException;
import com.hivemq.spi.callback.registry.CallbackRegistry;
import com.hivemq.spi.callback.security.AfterLoginCallback;
import com.hivemq.spi.security.ClientData;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class PluginAfterLoginCallbackHandler extends ChannelInboundHandlerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(PluginAfterLoginCallbackHandler.class);
    private final CallbackRegistry callbackRegistry;
    private final Metrics metrics;
    private final CallbackExecutor callbackExecutor;

    public PluginAfterLoginCallbackHandler(CallbackRegistry callbackRegistry,
                                           Metrics metrics,
                                           CallbackExecutor callbackExecutor) {
        this.callbackRegistry = callbackRegistry;
        this.metrics = metrics;
        this.callbackExecutor = callbackExecutor;
    }

    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof PluginAfterLogin) {
            afterLogin(ctx, (PluginAfterLogin) evt);
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    private void afterLogin(ChannelHandlerContext ctx, PluginAfterLogin event) {
        boolean available = this.callbackRegistry.isAvailable(AfterLoginCallback.class);
        if (!available) {
            return;
        }
        ClientData clientData = event.getClientData();
        List<AfterLoginCallback> callbacks = this.callbackRegistry.getCallbacks(AfterLoginCallback.class);
        callbacks.forEach(callback ->
                submitTask(ctx, callback, clientData, event.isAccepted(), event.getException())
        );
    }

    private void submitTask(ChannelHandlerContext ctx,
                            AfterLoginCallback callback,
                            ClientData clientData,
                            boolean accepted,
                            AuthenticationException exception) {
        this.callbackExecutor.submit(createTask(callback, clientData, accepted, exception));
    }

    @NotNull
    @VisibleForTesting
    Task createTask(AfterLoginCallback callback,
                    ClientData clientData,
                    boolean accepted,
                    AuthenticationException exception) {
        return new Task(callback, clientData, accepted, exception, this.metrics);
    }

    @VisibleForTesting
    static class Task implements RunnableTask {
        private final AfterLoginCallback callback;
        private final ClientData clientData;
        private final boolean accepted;
        private final AuthenticationException exception;
        private final Metrics metrics;

        public Task(AfterLoginCallback callback,
                    ClientData clientData,
                    boolean accepted,
                    AuthenticationException exception,
                    Metrics metrics) {
            this.callback = callback;
            this.clientData = clientData;
            this.accepted = accepted;
            this.exception = exception;
            this.metrics = metrics;
        }

        public void run() {
            Timer.Context timer = null;
            try {
                if (this.accepted) {
                    timer = this.metrics.pluginTimerAfterLoginSuccess().time();
                    this.callback.afterSuccessfulLogin(this.clientData);
                } else {
                    timer = this.metrics.pluginTimerAfterLoginFailed().time();
                    this.callback.afterFailedLogin(this.exception, this.clientData);
                }
                timer.stop();
            } catch (Throwable t) {
                if (timer != null) {
                    timer.stop();
                }
                LOGGER.error("Unhandled Exception in AfterLoginCallback {}.", this.callback.getClass());
                PluginExceptionUtils.log(t);
            }
        }

        @NotNull
        public Class callbackType() {
            return this.callback.getClass();
        }
    }
}
