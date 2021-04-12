package af;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import t1.ClientDisconnectedRequest;

public class ClientDisconnectedRequestSerializer
        extends Serializer<ClientDisconnectedRequest> {
    @Override
    public void write(Kryo kryo, Output output, ClientDisconnectedRequest object) {
        output.writeString(object.getClientId());
        output.writeString(object.getConnectedNode());
    }

    @Override
    public ClientDisconnectedRequest read(Kryo kryo, Input input, Class<ClientDisconnectedRequest> type) {
        String clientId = input.readString();
        String connectedNode = input.readString();
        return new ClientDisconnectedRequest(clientId, connectedNode);
    }
}
