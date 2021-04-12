package cg;

import am1.Metrics;
import bt1.PluginOnConnAckSend;
import ca.CallbackExecutor;
import ca.RunnableTask;
import cb1.PluginExceptionUtils;
import com.codahale.metrics.Timer;
import com.hivemq.spi.annotations.NotNull;
import com.hivemq.spi.callback.lowlevel.OnConnAckSend;
import com.hivemq.spi.callback.registry.CallbackRegistry;
import com.hivemq.spi.message.ConnAck;
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
public class PluginOnConnAckSendCallbackHandler
        extends ChannelInboundHandlerAdapter {
    private final CallbackRegistry callbackRegistry;
    private final Metrics metrics;
    private final CallbackExecutor callbackExecutor;

    @Inject
    PluginOnConnAckSendCallbackHandler(CallbackRegistry callbackRegistry,
                                       Metrics metrics,
                                       CallbackExecutor callbackExecutor) {
        this.callbackRegistry = callbackRegistry;
        this.metrics = metrics;
        this.callbackExecutor = callbackExecutor;
    }

    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (!(evt instanceof PluginOnConnAckSend)) {
            super.userEventTriggered(ctx, evt);
            return;
        }
        PluginOnConnAckSend event = (PluginOnConnAckSend) evt;
        ClientData clientData = event.getClientData();
        List<OnConnAckSend> callbacks = this.callbackRegistry.getCallbacks(OnConnAckSend.class);
        callbacks.forEach(callback ->
                this.callbackExecutor.submit(new Task(callback, clientData, event.getConnAck(), this.metrics))
        );
    }

    static class Task implements RunnableTask {
        private static final Logger LOGGER = LoggerFactory.getLogger(Task.class);
        private final OnConnAckSend callback;
        private final ClientData clientData;
        private final ConnAck connAck;
        private final Metrics metrics;

        Task(OnConnAckSend callback,
             ClientData clientData,
             ConnAck connAck,
             Metrics metrics) {
            this.callback = callback;
            this.clientData = clientData;
            this.connAck = connAck;
            this.metrics = metrics;
        }

        public void run() {
            Timer.Context timer = this.metrics.pluginTimerConnAckSend().time();
            try {
                this.callback.onConnackSend(this.connAck, this.clientData);
                timer.stop();
            } catch (Throwable e) {
                LOGGER.error("Unhandled Exception in OnConnAckSendCallback {}.", this.callback.getClass());
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
