package af;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.google.common.collect.ImmutableList;
import com.hivemq.spi.message.MessageWithId;
import u1.MessageFlow;

public class MessageFlowSerializer extends Serializer<MessageFlow> {

    @Override
    public void write(Kryo kryo, Output output, MessageFlow object) {
        kryo.writeClassAndObject(output, object.getMessages());
    }

    @Override
    public MessageFlow read(Kryo kryo, Input input, Class<MessageFlow> type) {
        ImmutableList<MessageWithId> messages = (ImmutableList<MessageWithId>) kryo.readClassAndObject(input);
        return new MessageFlow(messages);
    }
}
