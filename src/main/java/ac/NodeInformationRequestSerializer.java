package ac;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import y1.NodeInformationRequest;

public class NodeInformationRequestSerializer
        extends Serializer<NodeInformationRequest> {

    @Override
    public void write(Kryo kryo, Output output, NodeInformationRequest object) {
        output.writeString(object.getNode());
        output.writeString(object.getVersion());
    }

    @Override
    public NodeInformationRequest read(Kryo kryo, Input input, Class<NodeInformationRequest> type) {
        String node = input.readString();
        String version = input.readString();
        return new NodeInformationRequest(node, version);
    }
}
