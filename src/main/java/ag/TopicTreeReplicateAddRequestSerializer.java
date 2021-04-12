package ag;

import aa.TopicTreeReplicateAddRequest;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.hivemq.spi.message.Topic;

public class TopicTreeReplicateAddRequestSerializer
        extends Serializer<TopicTreeReplicateAddRequest> {

    @Override
    public void write(Kryo kryo, Output output, TopicTreeReplicateAddRequest object) {
        output.writeString(object.getSubscriber());
        output.writeString(object.getSegmentKey());
        kryo.writeObject(output, object.getTopic());
        output.write(object.getShared());
        output.writeString(object.getGroupId());
    }

    @Override
    public TopicTreeReplicateAddRequest read(Kryo kryo, Input input, Class<TopicTreeReplicateAddRequest> type) {
        String subscriber = input.readString();
        String segmentKey = input.readString();
        Topic topic = kryo.readObject(input, Topic.class);
        byte shared = input.readByte();
        String groupId = input.readString();
        return new TopicTreeReplicateAddRequest(topic, subscriber, segmentKey, shared, groupId);
    }
}
