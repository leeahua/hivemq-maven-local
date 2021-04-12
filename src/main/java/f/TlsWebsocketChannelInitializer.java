package f;

import cc1.WebsocketTransportChannelInitializer;
import com.hivemq.spi.services.configuration.entity.Tls;
import com.hivemq.spi.services.configuration.entity.TlsWebsocketListener;
import ct.SslFactory;
import ct.SslTransportChannelInitializer;
import e.ChannelPipelineDependencies;
import io.netty.channel.Channel;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.concurrent.EventExecutorGroup;

public class TlsWebsocketChannelInitializer extends AbstractChannelInitializer {
    private final TlsWebsocketListener listener;
    private final SslFactory sslFactory;

    public TlsWebsocketChannelInitializer(ChannelPipelineDependencies dependencies,
                                          TlsWebsocketListener listener,
                                          SslFactory sslFactory,
                                          EventExecutorGroup eventExecutorGroup) {
        super(dependencies, eventExecutorGroup);
        this.listener = listener;
        this.sslFactory = sslFactory;
    }

    protected void addIdleHandler(Channel channel) {
    }

    protected void supperAddIdleHandler(Channel channel) {
        super.addIdleHandler(channel);
    }

    protected void initTransportChannel(Channel channel) {
        Tls tls = this.listener.getTls();
        SslHandler sslHandler = this.sslFactory.createSslHandler(channel, tls);
        sslHandler.handshakeFuture().addListener(future -> supperAddIdleHandler(channel));
        new SslTransportChannelInitializer(sslHandler, this.listener.getTls()).initChannel(channel);
        new WebsocketTransportChannelInitializer(this.listener).initChannel(channel);
    }
}
