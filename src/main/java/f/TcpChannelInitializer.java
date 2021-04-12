package f;

import com.hivemq.spi.services.configuration.entity.TcpListener;
import ct.NoSslHandler;
import e.ChannelPipelineDependencies;
import e.Pipelines;
import io.netty.channel.Channel;
import io.netty.util.concurrent.EventExecutorGroup;

import javax.inject.Provider;

public class TcpChannelInitializer extends AbstractChannelInitializer {
    private final TcpListener listener;
    private final Provider<NoSslHandler> noSslHandlerProvider;

    public TcpChannelInitializer(ChannelPipelineDependencies dependencies,
                                 TcpListener listener,
                                 Provider<NoSslHandler> noSslHandlerProvider,
                                 EventExecutorGroup eventExecutorGroup) {
        super(dependencies, eventExecutorGroup);
        this.listener = listener;
        this.noSslHandlerProvider = noSslHandlerProvider;
    }

    protected void initTransportChannel(Channel channel) {
        channel.pipeline().addBefore(Pipelines.ALL_CHANNEL_GROUP_HANDLER, Pipelines.NO_SSL_HANDLER, this.noSslHandlerProvider.get());
    }
}
