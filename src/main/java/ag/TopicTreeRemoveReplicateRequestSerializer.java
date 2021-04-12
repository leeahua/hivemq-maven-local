package ag;

import aa.TopicTreeRemoveReplicateRequest;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

public class TopicTreeRemoveReplicateRequestSerializer
        extends Serializer<TopicTreeRemoveReplicateRequest> {
    @Override
    public void write(Kryo kryo, Output output, TopicTreeRemoveReplicateRequest object) {
        output.writeString(object.getSegmentKey());
        output.writeString(object.getTopic());
        output.writeString(object.getSubscriber());
    }

    @Override
    public TopicTreeRemoveReplicateRequest read(Kryo kryo, Input input, Class<TopicTreeRemoveReplicateRequest> type) {
        String segmentKey = input.readString();
        String topic = input.readString();
        String subscriber = input.readString();
        return new TopicTreeRemoveReplicateRequest(segmentKey, topic, subscriber);
    }
}
