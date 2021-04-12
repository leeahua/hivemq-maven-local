package af;

import ak.VectorClock;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import z1.ClientSessionSubscriptionRemoveAllReplicateRequest;

public class ClientSessionSubscriptionRemoveAllReplicateRequestSerializer
        extends Serializer<ClientSessionSubscriptionRemoveAllReplicateRequest> {

    @Override
    public void write(Kryo kryo, Output output, ClientSessionSubscriptionRemoveAllReplicateRequest object) {
        output.writeLong(object.getTimestamp());
        kryo.writeObject(output, object.getVectorClock());
        output.writeString(object.getClientId());
    }

    @Override
    public ClientSessionSubscriptionRemoveAllReplicateRequest read(Kryo kryo, Input input, Class<ClientSessionSubscriptionRemoveAllReplicateRequest> type) {
        long timestamp = input.readLong();
        VectorClock vectorClock = kryo.readObject(input, VectorClock.class);
        String clientId = input.readString();
        return new ClientSessionSubscriptionRemoveAllReplicateRequest(timestamp, vectorClock, clientId);
    }
}
