package cx;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLException;
import java.io.IOException;
import java.nio.channels.ClosedChannelException;

@ChannelHandler.Sharable
public class UpdateErrorHandler extends ChannelHandlerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateErrorHandler.class);

    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (cause instanceof SSLException) {
            LOGGER.trace("Update check SSLException", cause);
            return;
        }
        if (cause instanceof ClosedChannelException) {
            return;
        }
        if (cause instanceof IOException) {
            LOGGER.trace("Update check IOException", cause);
            return;
        }
        if (cause instanceof IllegalArgumentException) {
            LOGGER.trace("Update check IOException", cause);
            return;
        }
        LOGGER.debug("update check failed: {}", ExceptionUtils.getStackTrace(cause));
    }
}
