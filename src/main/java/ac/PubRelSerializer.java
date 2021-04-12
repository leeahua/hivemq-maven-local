package ac;

import bi.CachedMessages;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.hivemq.spi.message.PubRel;

public class PubRelSerializer extends Serializer<PubRel> {
    private final CachedMessages cachedMessages = CachedMessages.INSTANCE;

    @Override
    public void write(Kryo kryo, Output output, PubRel object) {
        output.writeInt(object.getMessageId());
    }

    @Override
    public PubRel read(Kryo kryo, Input input, Class<PubRel> type) {
        int messageId = input.readInt();
        return this.cachedMessages.getPubRel(messageId);
    }
}
