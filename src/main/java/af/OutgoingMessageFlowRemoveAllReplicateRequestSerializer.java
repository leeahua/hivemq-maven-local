package af;

import ak.VectorClock;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import u1.OutgoingMessageFlowRemoveAllReplicateRequest;

public class OutgoingMessageFlowRemoveAllReplicateRequestSerializer
        extends Serializer<OutgoingMessageFlowRemoveAllReplicateRequest> {

    @Override
    public void write(Kryo kryo, Output output, OutgoingMessageFlowRemoveAllReplicateRequest object) {
        output.writeString(object.getClientId());
        kryo.writeObject(output, object.getVectorClock());
    }

    @Override
    public OutgoingMessageFlowRemoveAllReplicateRequest read(Kryo kryo, Input input, Class<OutgoingMessageFlowRemoveAllReplicateRequest> type) {
        String clientId = input.readString();
        VectorClock vectorClock = kryo.readObject(input, VectorClock.class);
        return new OutgoingMessageFlowRemoveAllReplicateRequest(clientId, vectorClock);
    }
}
