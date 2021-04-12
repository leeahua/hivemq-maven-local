package ac;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.google.common.collect.ImmutableSet;
import com.hivemq.spi.message.Topic;
import y.ClientSubscriptions;

public class ClientSessionSubscriptionsSerializer
        extends Serializer<ClientSubscriptions> {

    @Override
    public void write(Kryo kryo, Output output, ClientSubscriptions object) {
        kryo.writeClassAndObject(output, object.getSubscriptions());
    }

    @Override
    public ClientSubscriptions read(Kryo kryo, Input input, Class<ClientSubscriptions> type) {
        ImmutableSet<Topic> subscriptions = (ImmutableSet<Topic>) kryo.readClassAndObject(input);
        return new ClientSubscriptions(subscriptions);
    }
}
