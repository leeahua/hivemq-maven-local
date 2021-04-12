package ac;

import ak.VectorClock;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import java.util.HashMap;

public class VectorClockSerializer extends Serializer<VectorClock> {

    @Override
    public void write(Kryo kryo, Output output, VectorClock object) {
        kryo.writeClassAndObject(output, object.getVectors());
    }

    @Override
    public VectorClock read(Kryo kryo, Input input, Class<VectorClock> type) {
        HashMap<String, Integer> vectors = (HashMap<String, Integer>) kryo.readClassAndObject(input);
        return new VectorClock(vectors);
    }
}
