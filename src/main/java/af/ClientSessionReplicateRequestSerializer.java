package af;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import ak.VectorClock;
import v.ClientSession;
import t1.ClientSessionReplicateRequest;

public class ClientSessionReplicateRequestSerializer
        extends Serializer<ClientSessionReplicateRequest> {

    @Override
    public void write(Kryo kryo, Output output, ClientSessionReplicateRequest object) {
        output.writeLong(object.getTimestamp());
        kryo.writeObjectOrNull(output, object.getVectorClock(), VectorClock.class);
        output.writeString(object.getClientId());
        kryo.writeObject(output, object.getClientSession());
    }

    @Override
    public ClientSessionReplicateRequest read(Kryo kryo, Input input, Class<ClientSessionReplicateRequest> type) {
        long timestamp = input.readLong();
        VectorClock vectorClock = kryo.readObjectOrNull(input, VectorClock.class);
        String clientId = input.readString();
        ClientSession clientSession = kryo.readObject(input, ClientSession.class);
        return new ClientSessionReplicateRequest(timestamp, vectorClock, clientId, clientSession);
    }
}
