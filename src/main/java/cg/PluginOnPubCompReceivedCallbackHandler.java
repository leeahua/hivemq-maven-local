package cg;

import am1.Metrics;
import ca.CallbackExecutor;
import ca.RunnableTask;
import cb1.ChannelUtils;
import cb1.PluginExceptionUtils;
import com.codahale.metrics.Timer;
import com.hivemq.spi.annotations.NotNull;
import com.hivemq.spi.callback.lowlevel.OnPubCompReceived;
import com.hivemq.spi.callback.registry.CallbackRegistry;
import com.hivemq.spi.message.PubComp;
import com.hivemq.spi.security.ClientData;
import cs.ClientToken;
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
public class PluginOnPubCompReceivedCallbackHandler extends ChannelInboundHandlerAdapter {
    private final CallbackRegistry callbackRegistry;
    private final Metrics metrics;
    private final CallbackExecutor callbackExecutor;

    @Inject
    PluginOnPubCompReceivedCallbackHandler(CallbackRegistry callbackRegistry, Metrics metrics, CallbackExecutor callbackExecutor) {
        this.callbackRegistry = callbackRegistry;
        this.metrics = metrics;
        this.callbackExecutor = callbackExecutor;
    }

    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof PubComp &&
                this.callbackRegistry.isAvailable(OnPubCompReceived.class)) {
            List<OnPubCompReceived> callbacks = this.callbackRegistry.getCallbacks(OnPubCompReceived.class);
            ClientToken clientToken = ChannelUtils.clientToken(ctx.channel());
            callbacks.stream()
                    .map(callback -> new Task(callback, clientToken, (PubComp) msg, this.metrics))
                    .forEach(this.callbackExecutor::submit);
        }
        super.channelRead(ctx, msg);
    }

    static class Task implements RunnableTask {
        private static final Logger LOGGER = LoggerFactory.getLogger(Task.class);
        private final OnPubCompReceived callbacks;
        private final ClientData clientData;
        private final PubComp pubComp;
        private final Metrics metrics;

        Task(OnPubCompReceived callbacks, ClientData clientData, PubComp pubComp, Metrics metrics) {
            this.callbacks = callbacks;
            this.clientData = clientData;
            this.pubComp = pubComp;
            this.metrics = metrics;
        }

        public void run() {
            Timer.Context timer = this.metrics.pluginTimerPubCompReceived().time();
            try {
                this.callbacks.onPubcompReceived(this.pubComp, this.clientData);
                timer.stop();
            } catch (Throwable e) {
                LOGGER.error("Unhandled Exception in OnPubcompReceivedCallback {}.", this.callbacks.getClass());
                timer.stop();
                PluginExceptionUtils.log(e);
            }
        }

        @NotNull
        public Class callbackType() {
            return this.callbacks.getClass();
        }
    }
}
