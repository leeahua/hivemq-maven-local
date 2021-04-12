package ac;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.google.common.collect.ImmutableMap;

import java.util.HashMap;
import java.util.Map;

public class ImmutableMapSerializer extends Serializer<ImmutableMap> {

    @Override
    public void write(Kryo kryo, Output output, ImmutableMap object) {
        kryo.writeClassAndObject(output, new HashMap(object));
    }

    @Override
    public ImmutableMap read(Kryo kryo, Input input, Class<ImmutableMap> type) {
        Map map = (Map) kryo.readClassAndObject(input);
        return ImmutableMap.copyOf(map);
    }
}
