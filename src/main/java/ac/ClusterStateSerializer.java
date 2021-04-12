package ac;

import ah.ClusterState;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

public class ClusterStateSerializer extends Serializer<ClusterState> {

    @Override
    public void write(Kryo kryo, Output output, ClusterState object) {
        output.writeInt(object.getValue());
    }

    @Override
    public ClusterState read(Kryo kryo, Input input, Class<ClusterState> type) {
        int value = input.readInt();
        return ClusterState.valueOf(value);
    }
}
