package cg;

import am1.Metrics;
import ca.CallbackExecutor;
import ca.RunnableTask;
import cb1.ChannelUtils;
import cb1.PluginExceptionUtils;
import com.codahale.metrics.Timer;
import com.hivemq.spi.annotations.NotNull;
import com.hivemq.spi.callback.lowlevel.OnPubCompSend;
import com.hivemq.spi.callback.registry.CallbackRegistry;
import com.hivemq.spi.message.PubComp;
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
public class PluginOnPubCompSendCallbackHandler extends ChannelOutboundHandlerAdapter {
    private final CallbackRegistry callbackRegistry;
    private final Metrics metrics;
    private final CallbackExecutor callbackExecutor;

    @Inject
    PluginOnPubCompSendCallbackHandler(CallbackRegistry callbackRegistry, Metrics metrics, CallbackExecutor callbackExecutor) {
        this.callbackRegistry = callbackRegistry;
        this.metrics = metrics;
        this.callbackExecutor = callbackExecutor;
    }

    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof PubComp &&
                this.callbackRegistry.isAvailable(OnPubCompSend.class)) {
            List<OnPubCompSend> callbacks = this.callbackRegistry.getCallbacks(OnPubCompSend.class);
            ClientToken clientToken = ChannelUtils.clientToken(ctx.channel());
            callbacks.stream()
                    .map(callback->new Task(callback, clientToken, (PubComp) msg, this.metrics))
                    .forEach(this.callbackExecutor::submit);
        }
        super.write(ctx, msg, promise);
    }

    static class Task
            implements RunnableTask {
        private static final Logger LOGGER = LoggerFactory.getLogger(Task.class);
        private final OnPubCompSend callback;
        private final ClientData clientData;
        private final PubComp pubComp;
        private final Metrics metrics;

        Task(OnPubCompSend callback, ClientData clientData, PubComp pubComp, Metrics metrics) {
            this.callback = callback;
            this.clientData = clientData;
            this.pubComp = pubComp;
            this.metrics = metrics;
        }

        public void run() {
            Timer.Context timer = this.metrics.pluginTimerPubCompSend().time();
            try {
                this.callback.onPubcompSend(this.pubComp, this.clientData);
                timer.stop();
            } catch (Throwable e) {
                LOGGER.error("Unhandled Exception in OnPubcompSendCallback {}.", this.callback.getClass());
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
