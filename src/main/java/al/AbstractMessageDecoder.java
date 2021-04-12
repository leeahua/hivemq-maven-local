package al;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;

public abstract class AbstractMessageDecoder<T> {
    public abstract T decode(Channel channel, ByteBuf remainingByteBuf, byte fixedHeader);

    protected boolean validReservedFlagsBits(byte fixedHeader) {
        return (fixedHeader & 0xF) == 0;
    }
}
