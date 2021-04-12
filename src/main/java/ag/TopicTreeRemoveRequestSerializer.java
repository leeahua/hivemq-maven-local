package ag;

import aa.TopicTreeRemoveRequest;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

public class TopicTreeRemoveRequestSerializer
        extends Serializer<TopicTreeRemoveRequest> {

    @Override
    public void write(Kryo kryo, Output output, TopicTreeRemoveRequest object) {
        output.writeString(object.getSegmentKey());
        output.writeString(object.getTopic());
        output.writeString(object.getSubscriber());
    }

    @Override
    public TopicTreeRemoveRequest read(Kryo kryo, Input input, Class<TopicTreeRemoveRequest> type) {
        String segmentKey = input.readString();
        String topic = input.readString();
        String subscriber = input.readString();
        return new TopicTreeRemoveRequest(segmentKey, topic, subscriber);
    }
}
