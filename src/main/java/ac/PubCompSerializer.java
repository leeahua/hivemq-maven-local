package ac;

import bi.CachedMessages;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.hivemq.spi.message.PubComp;

public class PubCompSerializer extends Serializer<PubComp> {
    private final CachedMessages cachedMessages = CachedMessages.INSTANCE;

    @Override
    public void write(Kryo kryo, Output output, PubComp object) {
        output.writeInt(object.getMessageId());
    }

    @Override
    public PubComp read(Kryo kryo, Input input, Class<PubComp> type) {
        int messageId = input.readInt();
        return this.cachedMessages.getPubComp(messageId);
    }
}
