package am;

import com.hivemq.spi.message.PingResp;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class PingRespMessageEncoder extends MessageToByteEncoder<PingResp> {
    private static final byte FIXED_HEADER = -48;
    private static final byte REMAINING_LENGTH = 0;

    protected void encode(ChannelHandlerContext ctx, PingResp msg, ByteBuf out) {
        out.writeByte(FIXED_HEADER);
        out.writeByte(REMAINING_LENGTH);
    }
}
