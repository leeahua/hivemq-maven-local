package af;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import z1.ClientSessionSubscriptionGetRequest;

public class ClientSessionSubscriptionGetRequestSerializer
        extends Serializer<ClientSessionSubscriptionGetRequest> {

    @Override
    public void write(Kryo kryo, Output output, ClientSessionSubscriptionGetRequest object) {
        output.writeString(object.getClientId());
    }

    @Override
    public ClientSessionSubscriptionGetRequest read(Kryo kryo, Input input, Class<ClientSessionSubscriptionGetRequest> type) {
        String clientId = input.readString();
        return new ClientSessionSubscriptionGetRequest(clientId);
    }
}
