package af;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.google.common.collect.ImmutableMap;
import y1.ClusterContextRequest;

public class ClusterContextRequestSerializer extends Serializer<ClusterContextRequest> {

    @Override
    public void write(Kryo kryo, Output output, ClusterContextRequest object) {
        kryo.writeClassAndObject(output, object.getNodeInformations());
    }

    @Override
    public ClusterContextRequest read(Kryo kryo, Input input, Class<ClusterContextRequest> type) {
        ImmutableMap<String, String> nodeInformations = (ImmutableMap<String, String>) kryo.readClassAndObject(input);
        return new ClusterContextRequest(nodeInformations);
    }
}
