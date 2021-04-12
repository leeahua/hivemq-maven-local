package af;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import t1.NodeForPublishRequest;

public class NodeForPublishRequestSerializer
        extends Serializer<NodeForPublishRequest> {

    @Override
    public void write(Kryo kryo, Output output, NodeForPublishRequest object) {
        output.writeString(object.getClientId());
    }

    @Override
    public NodeForPublishRequest read(Kryo kryo, Input input, Class<NodeForPublishRequest> type) {
        String clientId = input.readString();
        return new NodeForPublishRequest(clientId);
    }
}
