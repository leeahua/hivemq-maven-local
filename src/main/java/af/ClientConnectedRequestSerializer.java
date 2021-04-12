package af;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import t1.ClientConnectedRequest;

public class ClientConnectedRequestSerializer
        extends Serializer<ClientConnectedRequest> {

    @Override
    public void write(Kryo kryo, Output output, ClientConnectedRequest object) {
        output.writeString(object.getClientId());
        output.writeString(object.getConnectedNode());
        output.writeBoolean(object.isPersistentSession());
    }

    @Override
    public ClientConnectedRequest read(Kryo kryo, Input input, Class<ClientConnectedRequest> type) {
        String clientId = input.readString();
        String connectedNode = input.readString();
        boolean persistentSession = input.readBoolean();
        return new ClientConnectedRequest(persistentSession, clientId, connectedNode);
    }
}
