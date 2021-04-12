package af;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.google.common.collect.ImmutableList;
import com.hivemq.spi.message.MessageWithId;
import u1.OutgoingMessageFlowPutRequest;

public class OutgoingMessageFlowPutRequestSerializer
        extends Serializer<OutgoingMessageFlowPutRequest> {

    @Override
    public void write(Kryo kryo, Output output, OutgoingMessageFlowPutRequest object) {
        output.writeString(object.getClientId());
        kryo.writeClassAndObject(output, object.getMessages());
    }

    @Override
    public OutgoingMessageFlowPutRequest read(Kryo kryo, Input input, Class<OutgoingMessageFlowPutRequest> type) {
        String clientId = input.readString();
        ImmutableList<MessageWithId> messages = (ImmutableList<MessageWithId>) kryo.readClassAndObject(input);
        return new OutgoingMessageFlowPutRequest(clientId, messages);
    }
}
