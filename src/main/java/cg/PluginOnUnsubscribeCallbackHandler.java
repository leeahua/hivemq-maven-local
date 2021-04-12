package cg;

import am1.Metrics;
import bz1.PluginOnUnsubscribe;
import ca.CallbackExecutor;
import ca.RunnableTask;
import cb1.PluginExceptionUtils;
import com.codahale.metrics.Timer;
import com.hivemq.spi.annotations.NotNull;
import com.hivemq.spi.callback.events.OnUnsubscribeCallback;
import com.hivemq.spi.callback.registry.CallbackRegistry;
import com.hivemq.spi.message.Unsubscribe;
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
public class PluginOnUnsubscribeCallbackHandler
        extends ChannelInboundHandlerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(PluginOnUnsubscribeCallbackHandler.class);
    private final CallbackRegistry callbackRegistry;
    private final Metrics metrics;
    private final CallbackExecutor callbackExecutor;

    @Inject
    PluginOnUnsubscribeCallbackHandler(CallbackRegistry callbackRegistry,
                                       Metrics metrics,
                                       CallbackExecutor callbackExecutor) {
        this.callbackRegistry = callbackRegistry;
        this.metrics = metrics;
        this.callbackExecutor = callbackExecutor;
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (!(evt instanceof PluginOnUnsubscribe)) {
            super.userEventTriggered(ctx, evt);
            return;
        }
        PluginOnUnsubscribe event = (PluginOnUnsubscribe) evt;
        ClientData clientData = event.getClientData();
        Unsubscribe unsubscribe = event.getUnsubscribe();
        List<OnUnsubscribeCallback> callbacks = this.callbackRegistry.getCallbacks(OnUnsubscribeCallback.class);
        callbacks.forEach(callback->
                this.callbackExecutor.submit(new Task(callback, clientData, unsubscribe, this.metrics))
        );
    }

    static class Task
            implements RunnableTask {
        private final OnUnsubscribeCallback callback;
        private final ClientData clientData;
        private final Unsubscribe unsubscribe;
        private final Metrics metrics;

        Task(OnUnsubscribeCallback callback,
             ClientData clientData,
             Unsubscribe unsubscribe,
             Metrics metrics) {
            this.callback = callback;
            this.clientData = clientData;
            this.unsubscribe = unsubscribe;
            this.metrics = metrics;
        }

        public void run() {
            Timer.Context timer = this.metrics.pluginTimerUnsubscribe().time();
            try {
                this.callback.onUnsubscribe(this.unsubscribe, this.clientData);
                timer.stop();
            } catch (Throwable e) {
                LOGGER.error("Unhandled Exception in OnUnsubscribeCallback {}", this.callback.getClass());
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
