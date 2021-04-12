package ac;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.apache.commons.lang3.tuple.ImmutablePair;

public class ImmutablePairSerializer extends Serializer<ImmutablePair<String, String>> {

    @Override
    public void write(Kryo kryo, Output output, ImmutablePair<String, String> object) {
        output.writeString(object.getLeft());
        output.writeString(object.getRight());
    }

    @Override
    public ImmutablePair<String, String> read(Kryo kryo, Input input, Class<ImmutablePair<String, String>> type) {
        String left = input.readString();
        String right = input.readString();
        return new ImmutablePair(left, right);
    }
}
