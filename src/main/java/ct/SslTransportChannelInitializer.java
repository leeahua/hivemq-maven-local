package ct;

import com.hivemq.spi.services.configuration.entity.Tls;
import e.Pipelines;
import io.netty.channel.Channel;
import io.netty.handler.ssl.SslHandler;

public class SslTransportChannelInitializer {
    private final SslHandler sslHandler;
    private final Tls tls;

    public SslTransportChannelInitializer(SslHandler sslHandler, Tls tls) {
        this.sslHandler = sslHandler;
        this.tls = tls;
    }

    public void initChannel(Channel channel) {
        channel.pipeline().addBefore(Pipelines.ALL_CHANNEL_GROUP_HANDLER, Pipelines.SSL_HANDLER, this.sslHandler);
        channel.pipeline().addAfter(Pipelines.SSL_HANDLER, Pipelines.SSL_EXCEPTION_HANDLER, new SslExceptionHandler());
        if (!Tls.ClientAuthMode.NONE.equals(this.tls.getClientAuthMode())) {
            channel.pipeline().addAfter(Pipelines.SSL_EXCEPTION_HANDLER, Pipelines.SSL_CLIENT_CERTIFICATE_HANDLER, new SslClientCertificateHandler(this.tls));
        }
    }
}
