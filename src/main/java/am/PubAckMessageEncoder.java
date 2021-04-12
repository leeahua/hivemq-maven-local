package am;

import com.hivemq.spi.message.PubAck;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class PubAckMessageEncoder extends MessageToByteEncoder<PubAck> {
    private static final byte FIXED_HEADER = 64;
    private static final byte REMAINING_LENGTH = 2;

    protected void encode(ChannelHandlerContext ctx, PubAck msg, ByteBuf out) {
        if (msg.getMessageId() == 0) {
            throw new IllegalArgumentException("Message ID must not be null");
        }
        out.writeByte(FIXED_HEADER);
        out.writeByte(REMAINING_LENGTH);
        out.writeShort(msg.getMessageId());
    }
}
