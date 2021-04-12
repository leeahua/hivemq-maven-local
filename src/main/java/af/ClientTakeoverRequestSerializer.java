package af;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import t1.ClientTakeoverRequest;

public class ClientTakeoverRequestSerializer
        extends Serializer<ClientTakeoverRequest> {

    @Override
    public void write(Kryo kryo, Output output, ClientTakeoverRequest object) {
        output.writeString(object.getClientId());
    }

    @Override
    public ClientTakeoverRequest read(Kryo kryo, Input input, Class<ClientTakeoverRequest> type) {
        String clientId = input.readString();
        return new ClientTakeoverRequest(clientId);
    }
}
