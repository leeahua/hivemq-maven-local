package ac;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.google.common.collect.ImmutableSet;

import java.util.HashSet;
import java.util.Set;

public class ImmutableSetSerializer extends Serializer<ImmutableSet> {

    @Override
    public void write(Kryo kryo, Output output, ImmutableSet object) {
        kryo.writeClassAndObject(output, new HashSet(object));
    }

    @Override
    public ImmutableSet read(Kryo kryo, Input input, Class<ImmutableSet> type) {
        Set set = (Set) kryo.readClassAndObject(input);
        return ImmutableSet.copyOf(set);
    }
}
