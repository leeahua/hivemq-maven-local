package cg;

import am1.Metrics;
import ca.CallbackExecutor;
import ca.RunnableTask;
import cb1.ChannelUtils;
import cb1.PluginExceptionUtils;
import com.codahale.metrics.Timer;
import com.hivemq.spi.annotations.NotNull;
import com.hivemq.spi.callback.lowlevel.OnSubAckSend;
import com.hivemq.spi.callback.registry.CallbackRegistry;
import com.hivemq.spi.message.SubAck;
import com.hivemq.spi.security.ClientData;
import cs.ClientToken;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;

@ChannelHandler.Sharable
@Singleton
public class PluginOnSubAckSendCallbackHandler
        extends ChannelOutboundHandlerAdapter {
    private final CallbackRegistry callbackRegistry;
    private final Metrics metrics;
    private final CallbackExecutor callbackExecutor;

    @Inject
    PluginOnSubAckSendCallbackHandler(CallbackRegistry callbackRegistry, Metrics metrics, CallbackExecutor callbackExecutor) {
        this.callbackRegistry = callbackRegistry;
        this.metrics = metrics;
        this.callbackExecutor = callbackExecutor;
    }

    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof SubAck &&
                this.callbackRegistry.isAvailable(OnSubAckSend.class)) {
            List<OnSubAckSend> callbacks = this.callbackRegistry.getCallbacks(OnSubAckSend.class);
            ClientToken clientToken = ChannelUtils.clientToken(ctx.channel());
            callbacks.stream()
                    .map(callback -> new Task(callback, clientToken, (SubAck) msg, this.metrics))
                    .forEach(this.callbackExecutor::submit);
        }
        super.write(ctx, msg, promise);
    }

    static class Task implements RunnableTask {
        private static final Logger LOGGER = LoggerFactory.getLogger(Task.class);
        private final OnSubAckSend callback;
        private final ClientData clientData;
        private final SubAck subAck;
        private final Metrics metrics;

        Task(OnSubAckSend callback, ClientData clientData, SubAck subAck, Metrics metrics) {
            this.callback = callback;
            this.clientData = clientData;
            this.subAck = subAck;
            this.metrics = metrics;
        }

        public void run() {
            Timer.Context timer = this.metrics.pluginTimerSubAckSend().time();
            try {
                this.callback.onSubackSend(this.subAck, this.clientData);
                timer.stop();
            } catch (Throwable e) {
                LOGGER.error("Unhandled Exception in OnSubackSendCallback {}.", this.callback.getClass());
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
