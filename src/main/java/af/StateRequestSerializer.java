package af;

import ah.ClusterState;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import y1.ClusterStateRequest;

public class StateRequestSerializer extends Serializer<ClusterStateRequest> {
    @Override
    public void write(Kryo kryo, Output output, ClusterStateRequest object) {
        output.writeInt(object.getState().getValue());
    }

    @Override
    public ClusterStateRequest read(Kryo kryo, Input input, Class<ClusterStateRequest> type) {
        int value = input.readInt();
        ClusterState state = ClusterState.valueOf(value);
        return new ClusterStateRequest(state);
    }
}
