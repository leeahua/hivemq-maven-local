package cg;

import am1.Metrics;
import ca.CallbackExecutor;
import ca.RunnableTask;
import cb1.ChannelUtils;
import cb1.PluginExceptionUtils;
import com.codahale.metrics.Timer;
import com.hivemq.spi.annotations.NotNull;
import com.hivemq.spi.callback.lowlevel.OnPubRecReceived;
import com.hivemq.spi.callback.registry.CallbackRegistry;
import com.hivemq.spi.message.PubRec;
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
public class PluginOnPubRecReceivedCallbackHandler
        extends ChannelInboundHandlerAdapter {
    private final CallbackRegistry callbackRegistry;
    private final Metrics metrics;
    private final CallbackExecutor callbackExecutor;

    @Inject
    PluginOnPubRecReceivedCallbackHandler(CallbackRegistry callbackRegistry,
                                          Metrics metrics,
                                          CallbackExecutor callbackExecutor) {
        this.callbackRegistry = callbackRegistry;
        this.metrics = metrics;
        this.callbackExecutor = callbackExecutor;
    }

    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof PubRec &&
                this.callbackRegistry.isAvailable(OnPubRecReceived.class)) {
            List<OnPubRecReceived> callbacks = this.callbackRegistry.getCallbacks(OnPubRecReceived.class);
            ClientToken clientToken = ChannelUtils.clientToken(ctx.channel());
            callbacks.stream()
                    .map(callback -> new Task(callback, clientToken, (PubRec) msg, this.metrics))
                    .forEach(this.callbackExecutor::submit);
        }
        super.channelRead(ctx, msg);
    }

    static class Task
            implements RunnableTask {
        private static final Logger LOGGER = LoggerFactory.getLogger(Task.class);
        private final OnPubRecReceived callback;
        private final ClientData clientData;
        private final PubRec pubRec;
        private final Metrics metrics;

        Task(OnPubRecReceived callback, ClientData clientData, PubRec pubRec, Metrics metrics) {
            this.callback = callback;
            this.clientData = clientData;
            this.pubRec = pubRec;
            this.metrics = metrics;
        }

        public void run() {
            Timer.Context timer = this.metrics.pluginTimerPubRecReceived().time();
            try {
                this.callback.onPubrecReceived(this.pubRec, this.clientData);
                timer.stop();
            } catch (Throwable localThrowable) {
                LOGGER.error("Unhandled Exception in OnPubrecReceivedCallback {}.", this.callback.getClass());
                timer.stop();
                PluginExceptionUtils.log(localThrowable);
            }
        }

        @NotNull
        public Class callbackType() {
            return this.callback.getClass();
        }
    }
}
