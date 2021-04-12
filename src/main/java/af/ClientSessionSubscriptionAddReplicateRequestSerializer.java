package af;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.hivemq.spi.message.Topic;
import ak.VectorClock;
import z1.ClientSessionSubscriptionAddReplicateRequest;

public class ClientSessionSubscriptionAddReplicateRequestSerializer
        extends Serializer<ClientSessionSubscriptionAddReplicateRequest> {
    @Override
    public void write(Kryo kryo, Output output, ClientSessionSubscriptionAddReplicateRequest object) {
        output.writeLong(object.getTimestamp());
        kryo.writeObject(output, object.getVectorClock());
        output.writeString(object.getClientId());
        kryo.writeObject(output, object.getTopic());
    }

    @Override
    public ClientSessionSubscriptionAddReplicateRequest read(Kryo kryo, Input input, Class<ClientSessionSubscriptionAddReplicateRequest> type) {
        long timestamp = input.readLong();
        VectorClock vectorClock = kryo.readObject(input, VectorClock.class);
        String clientId = input.readString();
        Topic topic = kryo.readObject(input, Topic.class);
        return new ClientSessionSubscriptionAddReplicateRequest(timestamp, vectorClock, clientId, topic);
    }
}
