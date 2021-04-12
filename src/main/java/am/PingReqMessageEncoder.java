package am;

import com.hivemq.spi.message.PingReq;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class PingReqMessageEncoder extends MessageToByteEncoder<PingReq> {
    private static final byte FIXED_HEADER = -64;
    private static final byte REMAINING_LENGTH = 0;

    protected void encode(ChannelHandlerContext ctx, PingReq msg, ByteBuf out) {
        out.writeByte(FIXED_HEADER);
        out.writeByte(REMAINING_LENGTH);
    }
}
