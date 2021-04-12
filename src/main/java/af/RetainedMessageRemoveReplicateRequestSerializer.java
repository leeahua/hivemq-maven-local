package af;

import ak.VectorClock;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import x1.RetainedMessageRemoveReplicateRequest;

public class RetainedMessageRemoveReplicateRequestSerializer
        extends Serializer<RetainedMessageRemoveReplicateRequest> {

    @Override
    public void write(Kryo kryo, Output output, RetainedMessageRemoveReplicateRequest object) {
        output.writeLong(object.getTimestamp());
        kryo.writeObject(output, object.getVectorClock());
        output.writeString(object.getTopic());
    }

    @Override
    public RetainedMessageRemoveReplicateRequest read(Kryo kryo, Input input, Class<RetainedMessageRemoveReplicateRequest> type) {
        long timestamp = input.readLong();
        VectorClock vectorClock = kryo.readObject(input, VectorClock.class);
        String topic = input.readString();
        return new RetainedMessageRemoveReplicateRequest(timestamp, vectorClock, topic);
    }
}
