package cb1;

import com.google.common.base.Preconditions;
import com.google.common.primitives.Longs;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class ByteUtils {

    public static boolean getBoolean(byte byteValue, int position) {
        Preconditions.checkArgument(position < 8);
        Preconditions.checkArgument(position >= 0);
        return (byteValue & 1 << position) != 0;
    }

    public static byte getByte(byte byteValue, int position, boolean value) {
        Preconditions.checkArgument(position < 8);
        Preconditions.checkArgument(position >= 0);
        if (value) {
            return getTrueByte(byteValue, position);
        }
        return getFalseByte(byteValue, position);
    }

    public static byte getTrueByte(byte byteValue, int position) {
        Preconditions.checkArgument(position < 8);
        Preconditions.checkArgument(position >= 0);
        return (byte) (byteValue | 1 << position);
    }

    public static byte getFalseByte(byte byteValue, int position) {
        Preconditions.checkArgument(position < 8);
        Preconditions.checkArgument(position >= 0);
        return (byte) (byteValue & (1 << position ^ 0xFFFFFFFF));
    }

    public static byte[] toByteArray(ByteBuf byteBuf) {
        Preconditions.checkNotNull(byteBuf);
        if (byteBuf.readableBytes() < 2) {
            return null;
        }
        int i = byteBuf.readUnsignedShort();
        if (byteBuf.readableBytes() < i) {
            return null;
        }
        byte[] arrayOfByte = new byte[i];
        byteBuf.readBytes(arrayOfByte);
        return arrayOfByte;
    }

    public static ByteBuf toByteBuf(byte[] bytes) {
        Preconditions.checkNotNull(bytes);
        ByteBuf byteBuf = Unpooled.buffer();
        byteBuf.writeShort(bytes.length);
        byteBuf.writeBytes(bytes);
        return byteBuf;
    }

    public static long getLong(byte[] bytes, int position) {
        return Longs.fromBytes(
                bytes[position],
                bytes[position + 1],
                bytes[position + 2],
                bytes[position + 3],
                bytes[position + 4],
                bytes[position + 5],
                bytes[position + 6],
                bytes[position + 7]);
    }
}
