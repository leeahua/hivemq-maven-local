package ag;

import aa.TopicTreeReplicateSegmentRequest;
import by.Segment;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.google.common.collect.ImmutableSet;

public class TopicTreeReplicateSegmentRequestSerializer
        extends Serializer<TopicTreeReplicateSegmentRequest> {

    @Override
    public void write(Kryo kryo, Output output, TopicTreeReplicateSegmentRequest object) {
        kryo.writeClassAndObject(output, object.getSegments());
    }

    @Override
    public TopicTreeReplicateSegmentRequest read(Kryo kryo, Input input, Class<TopicTreeReplicateSegmentRequest> type) {
        ImmutableSet<Segment> segments = (ImmutableSet<Segment>) kryo.readClassAndObject(input);
        return new TopicTreeReplicateSegmentRequest(segments);
    }
}
