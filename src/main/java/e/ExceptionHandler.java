package e;

import am1.Metrics;
import cb1.ChannelUtils;
import com.codahale.metrics.Meter;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.net.ssl.SSLException;
import java.io.IOException;
import java.nio.channels.ClosedChannelException;

@Singleton
@ChannelHandler.Sharable
public class ExceptionHandler extends ChannelHandlerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExceptionHandler.class);
    private final Metrics metrics;

    @Inject
    public ExceptionHandler(Metrics metrics) {
        this.metrics = metrics;
    }

    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        Channel channel = ctx.channel();
        if (cause instanceof SSLException) {
            this.metrics.totalExceptionRate().mark();
            mark(cause);
            LOGGER.trace("SSLException", cause);
            return;
        }
        if (cause instanceof ClosedChannelException) {
            mark(cause);
            return;
        }
        if (cause instanceof IOException) {
            this.metrics.totalExceptionRate().mark();
            mark(cause);
            LOGGER.trace("IOException", cause);
            return;
        }
        if (cause instanceof IllegalArgumentException) {
            this.metrics.totalExceptionRate().mark();
            mark(cause);
            LOGGER.trace("IOException", cause);
        } else {
            this.metrics.totalExceptionRate().mark();
            mark(cause);
            LOGGER.error("An unexpected error occurred for client with IP {}: {}",
                    ChannelUtils.remoteIP(channel).orElse("UNKNOWN"), ExceptionUtils.getStackTrace(cause));
        }
        if (channel != null) {
            channel.close();
        }
    }

    public void mark(Throwable cause) {
        String name = "com.hivemq.exceptions." + cause.getClass().getSimpleName();
        Meter meter = this.metrics.getMetricRegistry().meter(name);
        meter.mark();
    }
}
