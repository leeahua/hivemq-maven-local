package af;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import t1.ClientDataRequest;

public class ClientDataRequestSerializer
        extends Serializer<ClientDataRequest> {

    @Override
    public void write(Kryo kryo, Output output, ClientDataRequest object) {
        output.writeString(object.getClientId());
    }

    @Override
    public ClientDataRequest read(Kryo kryo, Input input, Class<ClientDataRequest> type) {
        String clientId = input.readString();
        return new ClientDataRequest(clientId);
    }
}
