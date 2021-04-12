package e;

import com.google.common.base.Preconditions;
import com.hivemq.spi.services.configuration.entity.Listener;
import com.hivemq.spi.services.configuration.entity.TcpListener;
import com.hivemq.spi.services.configuration.entity.TlsTcpListener;
import com.hivemq.spi.services.configuration.entity.TlsWebsocketListener;
import com.hivemq.spi.services.configuration.entity.WebsocketListener;
import ct.NoSslHandler;
import ct.SslFactory;
import f.AbstractChannelInitializer;
import f.TcpChannelInitializer;
import f.TlsTcpChannelInitializer;
import f.TlsWebsocketChannelInitializer;
import f.WebsocketChannelInitializer;
import io.netty.util.concurrent.EventExecutorGroup;

import javax.inject.Inject;
import javax.inject.Provider;

public class ChannelInitializerFactory {
    private final ChannelPipelineDependencies dependencies;
    private final SslFactory sslFactory;
    private final Provider<NoSslHandler> noSslHandlerProvider;
    private final EventExecutorGroup eventExecutorGroup;

    @Inject
    public ChannelInitializerFactory(ChannelPipelineDependencies dependencies,
                                     SslFactory sslFactory,
                                     Provider<NoSslHandler> noSslHandlerProvider,
                                     EventExecutorGroup eventExecutorGroup) {
        this.dependencies = dependencies;
        this.sslFactory = sslFactory;
        this.noSslHandlerProvider = noSslHandlerProvider;
        this.eventExecutorGroup = eventExecutorGroup;
    }

    public AbstractChannelInitializer create(Listener listener) {
        Preconditions.checkNotNull(listener, "Listener must not be null");
        if (listener instanceof TcpListener) {
            if (listener instanceof TlsTcpListener) {
                return create((TlsTcpListener) listener);
            }
            return create((TcpListener) listener);
        }
        if (listener instanceof WebsocketListener) {
            if (listener instanceof TlsWebsocketListener) {
                return create((TlsWebsocketListener) listener);
            }
            return create((WebsocketListener) listener);
        }
        throw new IllegalArgumentException("Unknown listener type");
    }

    protected AbstractChannelInitializer create(TcpListener listener) {
        return new TcpChannelInitializer(this.dependencies, listener, this.noSslHandlerProvider, this.eventExecutorGroup);
    }

    protected AbstractChannelInitializer create(TlsTcpListener listener) {
        return new TlsTcpChannelInitializer(this.dependencies, listener, this.sslFactory, this.eventExecutorGroup);
    }

    protected AbstractChannelInitializer create(WebsocketListener listener) {
        return new WebsocketChannelInitializer(this.dependencies, listener, this.noSslHandlerProvider, this.eventExecutorGroup);
    }

    protected AbstractChannelInitializer create(TlsWebsocketListener listener) {
        return new TlsWebsocketChannelInitializer(this.dependencies, listener, this.sslFactory, this.eventExecutorGroup);
    }
}
