package ac;

import ai.TopicSubscribers;
import bx.SubscriberWithQoS;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.google.common.collect.ImmutableSet;

public class TopicSubscribersSerializer extends Serializer<TopicSubscribers> {

    @Override
    public void write(Kryo kryo, Output output, TopicSubscribers object) {
        kryo.writeClassAndObject(output, object.getSubscribers());
    }

    @Override
    public TopicSubscribers read(Kryo kryo, Input input, Class<TopicSubscribers> type) {
        ImmutableSet<SubscriberWithQoS> subscribers = (ImmutableSet<SubscriberWithQoS>) kryo.readClassAndObject(input);
        return new TopicSubscribers(subscribers);
    }
}
