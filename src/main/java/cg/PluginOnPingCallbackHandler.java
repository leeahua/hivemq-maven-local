package cg;

import am1.Metrics;
import bv1.PluginOnPing;
import ca.CallbackExecutor;
import ca.RunnableTask;
import cb1.PluginExceptionUtils;
import com.codahale.metrics.Timer;
import com.hivemq.spi.annotations.NotNull;
import com.hivemq.spi.callback.lowlevel.OnPingCallback;
import com.hivemq.spi.callback.registry.CallbackRegistry;
import com.hivemq.spi.security.ClientData;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;

@ChannelHandler.Sharable
@Singleton
public class PluginOnPingCallbackHandler extends ChannelInboundHandlerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(PluginOnPingCallbackHandler.class);
    private final CallbackRegistry callbackRegistry;
    private final Metrics metrics;
    private final CallbackExecutor callbackExecutor;

    @Inject
    PluginOnPingCallbackHandler(CallbackRegistry callbackRegistry, Metrics metrics, CallbackExecutor callbackExecutor) {
        this.callbackRegistry = callbackRegistry;
        this.metrics = metrics;
        this.callbackExecutor = callbackExecutor;
    }

    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof PluginOnPing) {
            PluginOnPing event = (PluginOnPing) evt;
            ClientData clientData = event.getClientData();
            List<OnPingCallback> callbacks = this.callbackRegistry.getCallbacks(OnPingCallback.class);
            callbacks.stream()
                    .map(callback -> new Task(callback, clientData, this.metrics))
                    .forEach(this.callbackExecutor::submit);
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    static class Task implements RunnableTask {
        private final OnPingCallback callback;
        private final ClientData clientData;
        private final Metrics metrics;

        Task(OnPingCallback callback, ClientData clientData, Metrics metrics) {
            this.callback = callback;
            this.clientData = clientData;
            this.metrics = metrics;
        }

        public void run() {
            Timer.Context timer = this.metrics.pluginTimerPing().time();
            try {
                this.callback.onPingReceived(this.clientData);
                timer.stop();
            } catch (Throwable e) {
                LOGGER.error("Unhandled Exception in OnPingCallback {}.", this.callback.getClass());
                timer.stop();
                PluginExceptionUtils.log(e);
            }
        }

        @NotNull
        public Class callbackType() {
            return this.callback.getClass();
        }
    }
}
