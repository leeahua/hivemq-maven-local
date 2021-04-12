package af;

import ak.VectorClock;
import bz.RetainedMessage;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import x1.RetainedMessageAddReplicateRequest;

public class RetainedMessageAddReplicateRequestSerializer
        extends Serializer<RetainedMessageAddReplicateRequest> {

    @Override
    public void write(Kryo kryo, Output output, RetainedMessageAddReplicateRequest object) {
        output.writeLong(object.getTimestamp());
        kryo.writeObject(output, object.getVectorClock());
        output.writeString(object.getTopic());
        kryo.writeObject(output, object.getRetainedMessage());
    }

    @Override
    public RetainedMessageAddReplicateRequest read(Kryo kryo, Input input, Class<RetainedMessageAddReplicateRequest> type) {
        long timestamp = input.readLong();
        VectorClock vectorClock = kryo.readObject(input, VectorClock.class);
        String topic = input.readString();
        RetainedMessage retainedMessage = kryo.readObject(input, RetainedMessage.class);
        return new RetainedMessageAddReplicateRequest(timestamp, vectorClock, topic, retainedMessage);
    }
}
