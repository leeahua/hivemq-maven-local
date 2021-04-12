package cb1;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.nio.charset.StandardCharsets;

public class ByteBufUtils {

    public static String decode(ByteBuf data) {
        Preconditions.checkNotNull(data);
        if (data.readableBytes() < 2) {
            return null;
        }
        int length = data.readUnsignedShort();
        if (data.readableBytes() < length) {
            return null;
        }
        byte[] dataBytes = new byte[length];
        data.readBytes(dataBytes);
        return new String(dataBytes, StandardCharsets.UTF_8);
    }


    public static ByteBuf encode(String data) {
        Preconditions.checkNotNull(data);
        ByteBuf buffer = Unpooled.buffer();
        byte[] dataBytes = data.getBytes(Charsets.UTF_8);
        buffer.writeShort(dataBytes.length);
        buffer.writeBytes(dataBytes);
        return buffer;
    }
}
