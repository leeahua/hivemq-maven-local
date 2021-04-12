package af;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.hivemq.spi.message.Topic;
import z1.ClientSessionSubscriptionRemoveRequest;

public class ClientSessionSubscriptionRemoveRequestSerializer
        extends Serializer<ClientSessionSubscriptionRemoveRequest> {

    @Override
    public void write(Kryo kryo, Output output, ClientSessionSubscriptionRemoveRequest object) {
        output.writeLong(object.getTimestamp());
        output.writeString(object.getClientId());
        kryo.writeObject(output, object.getTopic());
    }

    @Override
    public ClientSessionSubscriptionRemoveRequest read(Kryo kryo, Input input, Class<ClientSessionSubscriptionRemoveRequest> type) {
        long timestamp = input.readLong();
        String clientId = input.readString();
        Topic topic = kryo.readObject(input, Topic.class);
        return new ClientSessionSubscriptionRemoveRequest(timestamp, clientId, topic);
    }
}
