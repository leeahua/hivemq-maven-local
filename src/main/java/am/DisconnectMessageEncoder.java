package am;

import com.hivemq.spi.message.Disconnect;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class DisconnectMessageEncoder extends MessageToByteEncoder<Disconnect> {
    private static final byte FIXED_HEADER = -32;
    private static final byte REMAINING_LENGTH = 0;

    protected void encode(ChannelHandlerContext ctx, Disconnect paramDisconnect, ByteBuf paramByteBuf) {
        paramByteBuf.writeByte(FIXED_HEADER);
        paramByteBuf.writeByte(REMAINING_LENGTH);
    }
}
