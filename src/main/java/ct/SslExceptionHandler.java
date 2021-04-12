package ct;

import cb1.AttributeKeys;
import cb1.ChannelUtils;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.ssl.NotSslRecordException;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;

public class SslExceptionHandler extends ChannelHandlerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(SslExceptionHandler.class);

    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (isNotSslRecord(cause, ctx)) {
            return;
        }
        if (cause.getCause() != null) {
            if (cause.getCause() instanceof SSLHandshakeException) {
                onSSLHandshakeException(ctx, cause);
                ctx.close();
                return;
            }
            if (cause.getCause() instanceof SSLException) {
                onSSLException(ctx, cause);
                ctx.close();
                return;
            }
        }
        ctx.fireExceptionCaught(cause);
    }

    private void onSSLException(ChannelHandlerContext ctx, Throwable cause) {
        if (LOGGER.isDebugEnabled()) {
            Throwable rootCause = ExceptionUtils.getRootCause(cause);
            String clientId = ctx.channel().attr(AttributeKeys.MQTT_CLIENT_ID).get();
            if (clientId != null) {
                LOGGER.debug("SSL message transmission for client {} failed: {}",
                        clientId, rootCause.getMessage());
            } else {
                LOGGER.debug("SSL message transmission failed for client with IP {}: {}",
                        ChannelUtils.remoteIP(ctx.channel()).orElse("UNKNOWN"), rootCause.getMessage());
            }
            LOGGER.trace("Original Exception", rootCause);
        }
    }

    private void onSSLHandshakeException(ChannelHandlerContext ctx, Throwable cause) {
        if (LOGGER.isDebugEnabled()) {
            Throwable rootCause = ExceptionUtils.getRootCause(cause);
            String clientId = ctx.channel().attr(AttributeKeys.MQTT_CLIENT_ID).get();
            if (clientId != null) {
                LOGGER.debug("SSL Handshake for client {} failed: {}",
                        clientId, rootCause.getMessage());
            } else {
                LOGGER.debug("SSL Handshake failed for client with IP {}: {}",
                        ChannelUtils.remoteIP(ctx.channel()).orElse("UNKNOWN"), rootCause.getMessage());
            }
            LOGGER.trace("Original Exception", rootCause);
        }
    }

    private boolean isNotSslRecord(Throwable cause, ChannelHandlerContext ctx) {
        if (!(cause instanceof NotSslRecordException)) {
            return false;
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Client {} sent data which is not SSL/TLS to a SSL/TLS listener. Disconnecting client.",
                    ChannelUtils.remoteIP(ctx.channel()).orElse("UNKNOWN"));
            LOGGER.trace("Original Exception:", cause);
            ctx.close();
        }
        return true;
    }
}
