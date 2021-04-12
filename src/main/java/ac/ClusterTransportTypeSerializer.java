package ac;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import k.ClusterTransport.Type;

public class ClusterTransportTypeSerializer extends Serializer<Type> {

    @Override
    public void write(Kryo kryo, Output output, Type object) {
        output.writeInt(object.getValue());
    }

    @Override
    public Type read(Kryo kryo, Input input, Class<Type> type) {
        int value = input.readInt();
        return Type.a(value);
    }
}
