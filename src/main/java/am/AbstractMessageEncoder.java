package am;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.MessageToByteEncoder;

public abstract class AbstractMessageEncoder<T> extends MessageToByteEncoder<T> {
    protected ByteBuf encodeRemainingLength(int remainingLength) {
        int length = remainingLength;
        ByteBuf byteBuf = Unpooled.buffer();
        do {
            int stepByte = (byte) (length % 128);
            length /= 128;
            if (length > 0) {
                stepByte = (byte) (stepByte | 0xFFFFFF80);
            }
            byteBuf.writeByte(stepByte);
        } while (length > 0);
        return byteBuf;
    }
}
