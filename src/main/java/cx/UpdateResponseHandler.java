package cx;

import ca1.Update;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.update.entity.UpdateResponse;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpResponse;

import javax.inject.Inject;
import javax.inject.Provider;

@ChannelHandler.Sharable
public class UpdateResponseHandler extends SimpleChannelInboundHandler<FullHttpResponse>
        implements UpdateAttributeKeys {
    private final Provider<ObjectMapper> objectMapperProvider;

    @Inject
    UpdateResponseHandler(@Update Provider<ObjectMapper> objectMapperProvider) {
        this.objectMapperProvider = objectMapperProvider;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpResponse msg) throws Exception {
        ByteBuf content = msg.content();
        byte[] contentBytes = new byte[content.readableBytes()];
        content.getBytes(content.readerIndex(), contentBytes);
        UpdateResponse response = this.objectMapperProvider.get().readValue(contentBytes, UpdateResponse.class);
        ctx.channel().attr(RESPONSE).set(response);
    }
}
