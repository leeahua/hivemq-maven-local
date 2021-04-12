package af;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.google.common.collect.ImmutableSet;
import j1.ClusterReplicateRequest;
import x1.RetainedMessagePutAllRequest;

public class RetainedMessagePutAllRequestSerializer
        extends Serializer<RetainedMessagePutAllRequest> {

    @Override
    public void write(Kryo kryo, Output output, RetainedMessagePutAllRequest object) {
        kryo.writeClassAndObject(output, object.getRequests());
    }

    @Override
    public RetainedMessagePutAllRequest read(Kryo kryo, Input input, Class<RetainedMessagePutAllRequest> type) {
        ImmutableSet<ClusterReplicateRequest> requests = (ImmutableSet<ClusterReplicateRequest>) kryo.readClassAndObject(input);
        return new RetainedMessagePutAllRequest(requests);
    }
}
