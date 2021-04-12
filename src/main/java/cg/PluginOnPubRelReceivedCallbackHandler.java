package cg;

import am1.Metrics;
import ca.CallbackExecutor;
import ca.RunnableTask;
import cb1.ChannelUtils;
import cb1.PluginExceptionUtils;
import com.codahale.metrics.Timer;
import com.hivemq.spi.annotations.NotNull;
import com.hivemq.spi.callback.lowlevel.OnPubRelReceived;
import com.hivemq.spi.callback.registry.CallbackRegistry;
import com.hivemq.spi.message.PubRel;
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
public class PluginOnPubRelReceivedCallbackHandler extends ChannelInboundHandlerAdapter {
    private final CallbackRegistry callbackRegistry;
    private final Metrics metrics;
    private final CallbackExecutor callbackExecutor;

    @Inject
    PluginOnPubRelReceivedCallbackHandler(CallbackRegistry callbackRegistry,
                                          Metrics metrics,
                                          CallbackExecutor callbackExecutor) {
        this.callbackRegistry = callbackRegistry;
        this.metrics = metrics;
        this.callbackExecutor = callbackExecutor;
    }

    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof PubRel &&
                this.callbackRegistry.isAvailable(OnPubRelReceived.class)) {
            List<OnPubRelReceived> callbacks = this.callbackRegistry.getCallbacks(OnPubRelReceived.class);
            ClientToken clientToken = ChannelUtils.clientToken(ctx.channel());
            callbacks.stream()
                    .map(callback -> new Task(callback, clientToken, (PubRel) msg, this.metrics))
                    .forEach(this.callbackExecutor::submit);
        }
        super.channelRead(ctx, msg);
    }

    static class Task implements RunnableTask {
        private static final Logger LOGGER = LoggerFactory.getLogger(Task.class);
        private final OnPubRelReceived callback;
        private final ClientData clientData;
        private final PubRel pubRel;
        private final Metrics metrics;

        Task(OnPubRelReceived callback, ClientData clientData, PubRel pubRel, Metrics metrics) {
            this.callback = callback;
            this.clientData = clientData;
            this.pubRel = pubRel;
            this.metrics = metrics;
        }

        public void run() {
            Timer.Context timer = this.metrics.pluginTimerPubRelReceived().time();
            try {
                this.callback.onPubrelReceived(this.pubRel, this.clientData);
                timer.stop();
            } catch (Throwable e) {
                LOGGER.error("Unhandled Exception in OnPubrelReceivedCallback {}.", this.callback.getClass());
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
