package cg;

import am1.Metrics;
import bq1.PluginOnDisconnect;
import ca.CallbackExecutor;
import ca.RunnableTask;
import cb1.PluginExceptionUtils;
import com.codahale.metrics.Timer;
import com.hivemq.spi.annotations.NotNull;
import com.hivemq.spi.callback.events.OnDisconnectCallback;
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
public class PluginOnDisconnectCallbackHandler extends ChannelInboundHandlerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(PluginOnDisconnectCallbackHandler.class);
    private final CallbackRegistry callbackRegistry;
    private final Metrics metrics;
    private final CallbackExecutor callbackExecutor;

    @Inject
    PluginOnDisconnectCallbackHandler(CallbackRegistry callbackRegistry,
                                      Metrics metrics,
                                      CallbackExecutor callbackExecutor) {
        this.callbackRegistry = callbackRegistry;
        this.metrics = metrics;
        this.callbackExecutor = callbackExecutor;
    }

    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (!(evt instanceof PluginOnDisconnect)) {
            super.userEventTriggered(ctx, evt);
            return;
        }
        PluginOnDisconnect event = (PluginOnDisconnect) evt;
        ClientData clientData = event.getClientData();
        boolean receivedDisconnect = event.isReceivedDisconnect();
        List<OnDisconnectCallback> callbacks = this.callbackRegistry.getCallbacks(OnDisconnectCallback.class);
        callbacks.forEach(callback ->
                this.callbackExecutor.submit(new Task(callback, clientData, receivedDisconnect, metrics))
        );
    }

    static class Task implements RunnableTask {
        private final OnDisconnectCallback callback;
        private final ClientData clientData;
        private final boolean receivedDisconnect;
        private final Metrics metrics;

        Task(OnDisconnectCallback callback,
             ClientData clientData,
             boolean receivedDisconnect,
             Metrics metrics) {
            this.callback = callback;
            this.clientData = clientData;
            this.receivedDisconnect = receivedDisconnect;
            this.metrics = metrics;
        }

        public void run() {
            Timer.Context timer = this.metrics.pluginTimerDisconnect().time();
            try {
                this.callback.onDisconnect(this.clientData, !this.receivedDisconnect);
                timer.stop();
            } catch (Throwable e) {
                LOGGER.error("Unhandled Exception in OnDisconnectCallback {}.", this.callback.getClass());
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
