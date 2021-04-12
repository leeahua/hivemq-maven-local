package ac;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.List;

public class ImmutableListSerializer extends Serializer<ImmutableList> {

    @Override
    public void write(Kryo kryo, Output output, ImmutableList object) {
        kryo.writeClassAndObject(output, new ArrayList(object));
    }

    @Override
    public ImmutableList read(Kryo kryo, Input input, Class<ImmutableList> type) {
        List list = (List) kryo.readClassAndObject(input);
        return ImmutableList.copyOf(list);
    }
}
