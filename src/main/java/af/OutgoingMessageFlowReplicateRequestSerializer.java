package af;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.google.common.collect.ImmutableList;
import u1.OutgoingMessageFlowPutReplicateRequest;
import u1.OutgoingMessageFlowReplicateRequest;

public class OutgoingMessageFlowReplicateRequestSerializer
        extends Serializer<OutgoingMessageFlowReplicateRequest> {

    @Override
    public void write(Kryo kryo, Output output, OutgoingMessageFlowReplicateRequest object) {
        kryo.writeClassAndObject(output, object.getRequests());
    }

    @Override
    public OutgoingMessageFlowReplicateRequest read(Kryo kryo, Input input, Class<OutgoingMessageFlowReplicateRequest> type) {
        ImmutableList<OutgoingMessageFlowPutReplicateRequest> requests = (ImmutableList<OutgoingMessageFlowPutReplicateRequest>) kryo.readClassAndObject(input);
        return new OutgoingMessageFlowReplicateRequest(requests);
    }
}
