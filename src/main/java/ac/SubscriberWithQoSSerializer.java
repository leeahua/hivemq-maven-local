package ac;

import bx.SubscriberWithQoS;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

public class SubscriberWithQoSSerializer extends Serializer<SubscriberWithQoS> {

    @Override
    public void write(Kryo kryo, Output output, SubscriberWithQoS object) {
        output.writeString(object.getSubscriber());
        output.writeInt(object.getQosNumber());
        output.writeInt(object.getShared());
        output.writeString(object.getGroupId());
    }

    @Override
    public SubscriberWithQoS read(Kryo kryo, Input input, Class<SubscriberWithQoS> type) {
        String subscriber = input.readString();
        int qoSNumber = input.readInt();
        byte shared = (byte) input.readInt();
        String groupId = input.readString();
        return new SubscriberWithQoS(subscriber, qoSNumber, shared, groupId);
    }
}
