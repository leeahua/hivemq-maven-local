package ac;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import v.ClientSession;

public class ClientSessionSerializer extends Serializer<ClientSession> {

    @Override
    public void write(Kryo kryo, Output output, ClientSession object) {
        output.writeBoolean(object.isConnected());
        output.writeBoolean(object.isPersistentSession());
        output.writeString(object.getConnectedNode());
    }

    @Override
    public ClientSession read(Kryo kryo, Input input, Class<ClientSession> type) {
        boolean connected = input.readBoolean();
        boolean persistentSession = input.readBoolean();
        String clusterId = input.readString();
        return new ClientSession(connected, persistentSession, clusterId);
    }
}
