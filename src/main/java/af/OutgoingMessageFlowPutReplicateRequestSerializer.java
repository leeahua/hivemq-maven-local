package af;

import ak.VectorClock;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.google.common.collect.ImmutableList;
import com.hivemq.spi.message.MessageWithId;
import u1.OutgoingMessageFlowPutReplicateRequest;

public class OutgoingMessageFlowPutReplicateRequestSerializer
        extends Serializer<OutgoingMessageFlowPutReplicateRequest> {

    @Override
    public void write(Kryo kryo, Output output, OutgoingMessageFlowPutReplicateRequest object) {
        output.writeString(object.getClientId());
        kryo.writeClassAndObject(output, object.getMessages());
        kryo.writeObject(output, object.getVectorClock());
    }

    @Override
    public OutgoingMessageFlowPutReplicateRequest read(Kryo kryo, Input input, Class<OutgoingMessageFlowPutReplicateRequest> type) {
        String clientId = input.readString();
        ImmutableList<MessageWithId> messages = (ImmutableList<MessageWithId>) kryo.readClassAndObject(input);
        VectorClock vectorClock = kryo.readObject(input, VectorClock.class);
        return new OutgoingMessageFlowPutReplicateRequest(clientId, messages, vectorClock);
    }
}
