package af;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.google.common.collect.ImmutableSet;
import z1.ClientSessionSubscriptionAllReplicateRequest;
import z1.ClientSessionSubscriptionReplicateRequest;

public class ClientSessionSubscriptionAllReplicateRequestSerializer
        extends Serializer<ClientSessionSubscriptionAllReplicateRequest> {

    @Override
    public void write(Kryo kryo, Output output, ClientSessionSubscriptionAllReplicateRequest object) {
        kryo.writeClassAndObject(output, object.getRequests());
    }

    @Override
    public ClientSessionSubscriptionAllReplicateRequest read(Kryo kryo, Input input, Class<ClientSessionSubscriptionAllReplicateRequest> type) {
        ImmutableSet<ClientSessionSubscriptionReplicateRequest> requests = (ImmutableSet<ClientSessionSubscriptionReplicateRequest>) kryo.readClassAndObject(input);
        return new ClientSessionSubscriptionAllReplicateRequest(requests);
    }
}
