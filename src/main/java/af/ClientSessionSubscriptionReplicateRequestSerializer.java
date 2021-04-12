package af;

import ak.VectorClock;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.google.common.collect.ImmutableSet;
import com.hivemq.spi.message.Topic;
import z1.ClientSessionSubscriptionReplicateRequest;

public class ClientSessionSubscriptionReplicateRequestSerializer
        extends Serializer<ClientSessionSubscriptionReplicateRequest> {

    @Override
    public void write(Kryo kryo, Output output, ClientSessionSubscriptionReplicateRequest object) {
        output.writeString(object.getClientId());
        output.writeLong(object.getTimestamp());
        kryo.writeObject(output, object.getVectorClock());
        kryo.writeClassAndObject(output, object.getSubscriptions());
    }

    @Override
    public ClientSessionSubscriptionReplicateRequest read(Kryo kryo, Input input, Class<ClientSessionSubscriptionReplicateRequest> type) {
        String clientId = input.readString();
        long timestamp = input.readLong();
        VectorClock vectorClock = kryo.readObject(input, VectorClock.class);
        ImmutableSet<Topic> subscriptions = (ImmutableSet<Topic>) kryo.readClassAndObject(input);
        return new ClientSessionSubscriptionReplicateRequest(timestamp, vectorClock, clientId, subscriptions);
    }
}
