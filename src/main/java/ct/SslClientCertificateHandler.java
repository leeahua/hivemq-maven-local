package ct;

import cb1.AttributeKeys;
import com.hivemq.spi.services.configuration.entity.Tls;
import e.Pipelines;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.ssl.SslHandshakeCompletionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLPeerUnverifiedException;
import java.security.cert.Certificate;

public class SslClientCertificateHandler extends ChannelInboundHandlerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(SslClientCertificateHandler.class);
    private final Tls tls;

    public SslClientCertificateHandler(Tls tls) {
        this.tls = tls;
    }

    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (!(evt instanceof SslHandshakeCompletionEvent)) {
            super.userEventTriggered(ctx, evt);
            return;
        }
        SslHandshakeCompletionEvent localSslHandshakeCompletionEvent = (SslHandshakeCompletionEvent) evt;
        if (!localSslHandshakeCompletionEvent.isSuccess()) {
            LOGGER.trace("Handshake failed", localSslHandshakeCompletionEvent.cause());
            return;
        }
        Channel channel = ctx.channel();
        try {
            SslHandler sslHandler = (SslHandler) channel.pipeline().get(Pipelines.SSL_HANDLER);
            Certificate[] certificates = sslHandler.engine().getSession().getPeerCertificates();
            SslClientCertificateImpl certificate = new SslClientCertificateImpl(certificates);
            channel.attr(AttributeKeys.AUTH_CERTIFICATE).set(certificate);
        } catch (SSLPeerUnverifiedException e) {
            onSSLPeerUnverifiedException(channel, e);
        } catch (ClassCastException e) {
            channel.close();
            throw new RuntimeException("Not able to get SslHandler from pipeline", e);
        }
        channel.pipeline().remove(this);
    }

    private void onSSLPeerUnverifiedException(Channel channel, SSLPeerUnverifiedException exception) {
        if ("peer not authenticated".equals(exception.getMessage())) {
            if (Tls.ClientAuthMode.REQUIRED.equals(this.tls.getClientAuthMode())) {
                LOGGER.error("Client certificate authentication forced but no client certificate was provided. Disconnecting.", exception);
                channel.close();
            } else if (Tls.ClientAuthMode.OPTIONAL.equals(this.tls.getClientAuthMode())) {
                LOGGER.debug("Client did not provide SSL certificate for authentication. Could not authenticate at application level");
            }
        } else {
            LOGGER.error("An error occurred. Disconnecting client", exception);
            channel.close();
        }
    }
}
