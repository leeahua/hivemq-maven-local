package ag;

import bx.SubscriberWithQoS;
import by.Segment;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.google.common.collect.ImmutableMap;

import java.util.Set;

public class SegmentSerializer extends Serializer<Segment> {

    @Override
    public void write(Kryo kryo, Output output, Segment object) {
        output.writeString(object.getKey());
        kryo.writeClassAndObject(output, object.getEntries());
    }

    @Override
    public Segment read(Kryo kryo, Input input, Class<Segment> type) {
        String key = input.readString();
        ImmutableMap<String, Set<SubscriberWithQoS>> subscriptions = (ImmutableMap<String, Set<SubscriberWithQoS>>) kryo.readClassAndObject(input);
        return new Segment(key, subscriptions);
    }
}
