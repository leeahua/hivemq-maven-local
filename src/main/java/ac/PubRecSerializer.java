package ac;

import bi.CachedMessages;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.hivemq.spi.message.PubRec;

public class PubRecSerializer extends Serializer<PubRec> {
    private final CachedMessages cachedMessages = CachedMessages.INSTANCE;

    @Override
    public void write(Kryo kryo, Output output, PubRec object) {
        output.writeInt(object.getMessageId());
    }

    @Override
    public PubRec read(Kryo kryo, Input input, Class<PubRec> type) {
        int messageId = input.readInt();
        return this.cachedMessages.getPubRec(messageId);
    }
}
