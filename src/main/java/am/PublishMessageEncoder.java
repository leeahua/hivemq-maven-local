package am;

import cb1.ByteBufUtils;
import com.hivemq.spi.message.Publish;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

public class PublishMessageEncoder extends AbstractMessageEncoder<Publish> {
    private static final byte FIXED_HEADER = 48;

    protected void encode(ChannelHandlerContext ctx, Publish msg, ByteBuf out) {
        int fixedHeader = FIXED_HEADER;
        int qoSNumber = msg.getQoS().getQosNumber();
        if (msg.isDuplicateDelivery()) {
            fixedHeader = (byte) (fixedHeader | 0x8);
        }
        if (msg.isRetain()) {
            fixedHeader = (byte) (fixedHeader | 0x1);
        }
        fixedHeader = (byte) (fixedHeader | qoSNumber << 1);
        out.writeByte(fixedHeader);
        ByteBuf byteBuf = ByteBufUtils.encode(msg.getTopic());
        if (qoSNumber > 0) {
            byteBuf.writeShort(msg.getMessageId());
        }
        byteBuf.writeBytes(msg.getPayload());
        out.writeBytes(encodeRemainingLength(byteBuf.writerIndex()));
        out.writeBytes(byteBuf);
    }
}
