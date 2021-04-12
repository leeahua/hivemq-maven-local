package ci;

import am1.Metrics;
import bs1.PluginOnInsufficientPermissionDisconnect;
import bs1.PluginOnInsufficientPermissionDisconnect.Type;
import ca.CallbackExecutor;
import ca.RunnableTask;
import cb1.PluginExceptionUtils;
import com.codahale.metrics.Timer;
import com.hivemq.spi.annotations.NotNull;
import com.hivemq.spi.callback.registry.CallbackRegistry;
import com.hivemq.spi.callback.security.OnInsufficientPermissionDisconnect;
import com.hivemq.spi.message.QoS;
import com.hivemq.spi.security.ClientData;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;

@Singleton
@ChannelHandler.Sharable
public class PluginOnInsufficientPermissionDisconnectCallbackHandler extends ChannelInboundHandlerAdapter {
    private final CallbackRegistry callbackRegistry;
    private final Metrics metrics;
    private final CallbackExecutor callbackExecutor;

    @Inject
    PluginOnInsufficientPermissionDisconnectCallbackHandler(
            CallbackRegistry callbackRegistry,
            Metrics metrics,
            CallbackExecutor callbackExecutor) {
        this.callbackRegistry = callbackRegistry;
        this.metrics = metrics;
        this.callbackExecutor = callbackExecutor;
    }

    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (!(evt instanceof PluginOnInsufficientPermissionDisconnect)) {
            super.userEventTriggered(ctx, evt);
            return;
        }
        PluginOnInsufficientPermissionDisconnect event = (PluginOnInsufficientPermissionDisconnect) evt;
        ClientData clientData = event.getClientData();
        List<OnInsufficientPermissionDisconnect> callbacks = this.callbackRegistry.getCallbacks(OnInsufficientPermissionDisconnect.class);
        callbacks.forEach(callback -> this.callbackExecutor.submit(
                new Task(callback, clientData, event.getType(), event.getTopic(), event.getQoS(), this.metrics))
        );
    }

    static class Task implements RunnableTask {
        private static final Logger LOGGER = LoggerFactory.getLogger(Task.class);
        private final OnInsufficientPermissionDisconnect callback;
        private final ClientData clientData;
        private final Type type;
        private final String topic;
        private final QoS qoS;
        private final Metrics metrics;

        public Task(OnInsufficientPermissionDisconnect callback,
                    ClientData clientData,
                    Type type,
                    String topic,
                    QoS qoS,
                    Metrics metrics) {
            this.callback = callback;
            this.clientData = clientData;
            this.type = type;
            this.topic = topic;
            this.qoS = qoS;
            this.metrics = metrics;
        }

        public void run() {
            Timer.Context timer = null;
            try {
                if (this.type == null) {
                    LOGGER.error("Type cannot be null for InsufficientPermissionsDisconnect, skipping execution");
                    return;
                }
                if (this.type == Type.PUBLISH) {
                    timer = this.metrics.pluginTimerPermissionsDisconnectPublish().time();
                    this.callback.onPublishDisconnect(this.clientData, this.topic, this.qoS);
                } else {
                    timer = this.metrics.pluginTimerPermissionsDisconnectSubscribe().time();
                    this.callback.onSubscribeDisconnect(this.clientData, this.topic, this.qoS);
                }
                timer.stop();
            } catch (Throwable t) {
                if (timer != null) {
                    timer.stop();
                }
                PluginExceptionUtils.log(t);
            }
        }

        @NotNull
        public Class callbackType() {
            return this.callback.getClass();
        }
    }
}
