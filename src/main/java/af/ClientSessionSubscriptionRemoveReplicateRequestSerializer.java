package af;

import ak.VectorClock;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.hivemq.spi.message.Topic;
import z1.ClientSessionSubscriptionRemoveReplicateRequest;

public class ClientSessionSubscriptionRemoveReplicateRequestSerializer
        extends Serializer<ClientSessionSubscriptionRemoveReplicateRequest> {
    @Override
    public void write(Kryo kryo, Output output, ClientSessionSubscriptionRemoveReplicateRequest object) {
        output.writeLong(object.getTimestamp());
        kryo.writeObject(output, object.getVectorClock());
        output.writeString(object.getClientId());
        kryo.writeObject(output, object.getTopic());
    }

    @Override
    public ClientSessionSubscriptionRemoveReplicateRequest read(Kryo kryo, Input input, Class<ClientSessionSubscriptionRemoveReplicateRequest> type) {
        long timestamp = input.readLong();
        VectorClock vectorClock = kryo.readObject(input, VectorClock.class);
        String clientId = input.readString();
        Topic topic = kryo.readObject(input, Topic.class);
        return new ClientSessionSubscriptionRemoveReplicateRequest(timestamp, vectorClock, clientId, topic);
    }
}
