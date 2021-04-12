package ac;

import ab.ClusterResponseCode;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

public class ClusterResponseCodeSerializer extends Serializer<ClusterResponseCode> {
    @Override
    public void write(Kryo kryo, Output output, ClusterResponseCode object) {
        output.writeInt(object.getValue());
    }

    @Override
    public ClusterResponseCode read(Kryo kryo, Input input, Class<ClusterResponseCode> type) {
        int value = input.readInt();
        return ClusterResponseCode.valueOf(value);
    }
}
