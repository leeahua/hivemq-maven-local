package ac;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.hivemq.spi.security.ClientData;
import cs.ClientToken;

public class ClientDataSerializer extends Serializer<ClientData> {
    @Override
    public void write(Kryo kryo, Output output, ClientData object) {
        output.writeString(object.getClientId());
        output.writeString(object.getUsername().orElse(null));
        output.writeBoolean(object.isBridge());
        output.writeBoolean(object.isAuthenticated());
    }

    @Override
    public ClientData read(Kryo kryo, Input input, Class<ClientData> type) {
        String clientId = input.readString();
        String username = input.readString();
        boolean bridge = input.readBoolean();
        boolean authenticated = input.readBoolean();
        ClientToken clientToken = new ClientToken(clientId, username, null, null, bridge, null);
        clientToken.setAuthenticated(authenticated);
        return clientToken;
    }
}
