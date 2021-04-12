package cg;

import am1.Metrics;
import bu.InternalPublish;
import bx1.PluginOnPublishSend;
import ca.CallbackExecutor;
import ca.RunnableTask;
import cb1.PluginExceptionUtils;
import com.codahale.metrics.Timer;
import com.hivemq.spi.annotations.NotNull;
import com.hivemq.spi.callback.events.OnPublishSend;
import com.hivemq.spi.callback.registry.CallbackRegistry;
import com.hivemq.spi.security.ClientData;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;

@ChannelHandler.Sharable
@Singleton
public class PluginOnPublishSendCallbackHandler extends ChannelInboundHandlerAdapter {
    private final CallbackRegistry callbackRegistry;
    private final Metrics metrics;
    private final CallbackExecutor callbackExecutor;

    @Inject
    PluginOnPublishSendCallbackHandler(CallbackRegistry callbackRegistry,
                                       Metrics metrics,
                                       CallbackExecutor callbackExecutor) {
        this.callbackRegistry = callbackRegistry;
        this.metrics = metrics;
        this.callbackExecutor = callbackExecutor;
    }

    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (!(evt instanceof PluginOnPublishSend)) {
            super.userEventTriggered(ctx, evt);
            return;
        }
        PluginOnPublishSend event = (PluginOnPublishSend) evt;
        ClientData clientData = event.getClientData();
        List<OnPublishSend> callbacks = this.callbackRegistry.getCallbacks(OnPublishSend.class);
        callbacks.forEach(callback->
                this.callbackExecutor.submit(new Task(callback, clientData, event.getPublish(), this.metrics))
        );
    }

    private static class Task
            implements RunnableTask {
        private final OnPublishSend callback;
        private final ClientData clientData;
        private final InternalPublish publish;
        private final Metrics metrics;

        public Task(OnPublishSend callback,
                    ClientData clientData,
                    InternalPublish publish,
                    Metrics metrics) {
            this.callback = callback;
            this.clientData = clientData;
            this.publish = publish;
            this.metrics = metrics;
        }

        public void run() {
            Timer.Context timer = this.metrics.pluginTimerPublishSend().time();
            try {
                this.callback.onPublishSend(this.publish, this.clientData);
                timer.stop();
            } catch (Throwable e) {
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
