package af;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import t1.ClientSessionGetRequest;

public class ClientSessionGetRequestSerializer
        extends Serializer<ClientSessionGetRequest> {

    @Override
    public void write(Kryo kryo, Output output, ClientSessionGetRequest object) {
        output.writeString(object.getClientId());
    }

    @Override
    public ClientSessionGetRequest read(Kryo kryo, Input input, Class<ClientSessionGetRequest> type) {
        String clientId = input.readString();
        return new ClientSessionGetRequest(clientId);
    }
}
