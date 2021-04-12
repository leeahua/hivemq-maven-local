package cc1;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.util.ReferenceCountUtil;

import java.util.List;

public class MqttWebsocketEncoder extends MessageToMessageEncoder<ByteBuf> {

    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws Exception {
        BinaryWebSocketFrame binaryWebSocketFrame = new BinaryWebSocketFrame(msg);
        ReferenceCountUtil.retain(binaryWebSocketFrame);
        out.add(binaryWebSocketFrame);
    }
}
