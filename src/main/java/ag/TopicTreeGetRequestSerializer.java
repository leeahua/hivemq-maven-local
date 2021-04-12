package ag;

import aa.TopicTreeGetRequest;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

public class TopicTreeGetRequestSerializer
        extends Serializer<TopicTreeGetRequest> {

    @Override
    public void write(Kryo kryo, Output output, TopicTreeGetRequest object) {
        output.writeString(object.getTopic());
        output.writeString(object.getSegmentKey());
    }

    @Override
    public TopicTreeGetRequest read(Kryo kryo, Input input, Class<TopicTreeGetRequest> type) {
        String topic = input.readString();
        String segmentKey = input.readString();
        return new TopicTreeGetRequest(segmentKey, topic);
    }
}
