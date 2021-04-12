package am;

import cb1.ByteBufUtils;
import com.hivemq.spi.message.Subscribe;
import com.hivemq.spi.message.Topic;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;

import java.util.List;

public class SubscribeMessageEncoder extends AbstractMessageEncoder<Subscribe> {
    private static final byte FIXED_HEADER = -126;

    protected void encode(ChannelHandlerContext ctx, Subscribe msg, ByteBuf out) {
        out.writeByte(FIXED_HEADER);
        ByteBuf buffer = Unpooled.buffer();
        buffer.writeShort(msg.getMessageId());
        List<Topic> topics = msg.getTopics();
        topics.forEach(topic -> {
            buffer.writeBytes(ByteBufUtils.encode(topic.getTopic()));
            buffer.writeByte(topic.getQoS().getQosNumber());
        });
        out.writeBytes(encodeRemainingLength(buffer.writerIndex()));
        out.writeBytes(buffer);
    }
}
