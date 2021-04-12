package am;

import com.hivemq.spi.message.PubComp;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class PubCompMessageEncoder extends MessageToByteEncoder<PubComp> {
    private static final byte FIXED_HEADER = 112;
    private static final byte REMAINING_LENGTH = 2;

    protected void encode(ChannelHandlerContext ctx, PubComp msg, ByteBuf out) {
        if (msg.getMessageId() == 0) {
            throw new IllegalArgumentException("Message ID must not be null");
        }
        out.writeByte(FIXED_HEADER);
        out.writeByte(REMAINING_LENGTH);
        out.writeShort(msg.getMessageId());
    }
}
