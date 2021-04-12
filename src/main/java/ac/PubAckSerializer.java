package ac;

import bi.CachedMessages;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.hivemq.spi.message.PubAck;

public class PubAckSerializer extends Serializer<PubAck> {
    private final CachedMessages cachedMessages = CachedMessages.INSTANCE;

    @Override
    public void write(Kryo kryo, Output output, PubAck object) {
        output.writeInt(object.getMessageId());
    }

    @Override
    public PubAck read(Kryo kryo, Input input, Class<PubAck> type) {
        int messageId = input.readInt();
        return this.cachedMessages.getPubAck(messageId);
    }
}
