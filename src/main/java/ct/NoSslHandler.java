package ct;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.ssl.SslHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class NoSslHandler extends ByteToMessageDecoder {
    private static final Logger LOGGER = LoggerFactory.getLogger(NoSslHandler.class);

    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        if (in.readableBytes() < 5) {
            return;
        }
        if (SslHandler.isEncrypted(in)) {
            LOGGER.debug("SSL connection on non-SSL listener, dropping connection.");
            in.clear();
            ctx.close();
        }
        ctx.pipeline().remove(this);
    }
}
