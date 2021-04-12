package af;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.google.common.collect.ImmutableSet;
import t1.ClientSessionPutAllRequest;
import t1.ClientSessionReplicateRequest;

public class ClientSessionPutAllRequestSerializer
        extends Serializer<ClientSessionPutAllRequest> {

    @Override
    public void write(Kryo kryo, Output output, ClientSessionPutAllRequest object) {
        kryo.writeClassAndObject(output, object.getRequests());
    }

    @Override
    public ClientSessionPutAllRequest read(Kryo kryo, Input input, Class<ClientSessionPutAllRequest> type) {
        ImmutableSet<ClientSessionReplicateRequest> requests = (ImmutableSet<ClientSessionReplicateRequest>) kryo.readClassAndObject(input);
        return new ClientSessionPutAllRequest(requests);
    }
}
