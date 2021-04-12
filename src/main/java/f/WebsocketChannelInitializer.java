package f;

import cc1.WebsocketTransportChannelInitializer;
import com.hivemq.spi.services.configuration.entity.WebsocketListener;
import ct.NoSslHandler;
import e.ChannelPipelineDependencies;
import e.Pipelines;
import io.netty.channel.Channel;
import io.netty.util.concurrent.EventExecutorGroup;

import javax.inject.Provider;

public class WebsocketChannelInitializer extends AbstractChannelInitializer {
    private final WebsocketListener listener;
    private final Provider<NoSslHandler> noSslHandlerProvider;

    public WebsocketChannelInitializer(ChannelPipelineDependencies dependencies,
                                       WebsocketListener listener,
                                       Provider<NoSslHandler> noSslHandlerProvider,
                                       EventExecutorGroup eventExecutorGroup) {
        super(dependencies, eventExecutorGroup);
        this.listener = listener;
        this.noSslHandlerProvider = noSslHandlerProvider;
    }

    protected void initTransportChannel(Channel channel) {
        channel.pipeline().addBefore(Pipelines.ALL_CHANNEL_GROUP_HANDLER, Pipelines.NO_SSL_HANDLER, this.noSslHandlerProvider.get());
        new WebsocketTransportChannelInitializer(this.listener).initChannel(channel);
    }
}
