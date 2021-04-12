package ac;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import k.ClusterDiscovery.Type;

public class ClusterDiscoveryTypeSerializer extends Serializer<Type> {

    @Override
    public void write(Kryo kryo, Output output, Type object) {
        output.writeInt(object.value());
    }

    @Override
    public Type read(Kryo kryo, Input input, Class<Type> clazz) {
        int value = input.readInt();
        return Type.valueOf(value);
    }
}
