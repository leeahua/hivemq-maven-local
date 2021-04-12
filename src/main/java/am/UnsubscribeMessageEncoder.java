package am;

import cb1.ByteBufUtils;
import com.hivemq.spi.message.Unsubscribe;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;

import java.util.List;

public class UnsubscribeMessageEncoder extends AbstractMessageEncoder<Unsubscribe> {
    private static final byte FIXED_HEADER = -94;

    protected void encode(ChannelHandlerContext ctx, Unsubscribe msg, ByteBuf out) {
        out.writeByte(FIXED_HEADER);
        ByteBuf buffer = Unpooled.buffer();
        buffer.writeShort(msg.getMessageId());
        List<String> topics = msg.getTopics();
        topics.forEach(topic -> buffer.writeBytes(ByteBufUtils.encode(topic)));
        out.writeBytes(encodeRemainingLength(buffer.writerIndex()));
        out.writeBytes(buffer);
    }
}
