package ag;

import aa.TopicTreeAddRequest;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.hivemq.spi.message.Topic;

public class TopicTreeAddRequestSerializer
        extends Serializer<TopicTreeAddRequest> {

    @Override
    public void write(Kryo kryo, Output output, TopicTreeAddRequest object) {
        output.writeString(object.getSegmentKey());
        output.writeString(object.getSubscriber());
        kryo.writeObject(output, object.getTopic());
        output.writeByte(object.getShared());
        output.writeString(object.getGroupId());
    }

    @Override
    public TopicTreeAddRequest read(Kryo kryo, Input input, Class<TopicTreeAddRequest> type) {
        String segmentKey = input.readString();
        String subscriber = input.readString();
        Topic topic = kryo.readObject(input, Topic.class);
        byte shared = input.readByte();
        String groupId = input.readString();
        return new TopicTreeAddRequest(segmentKey, topic, subscriber, shared, groupId);
    }
}
