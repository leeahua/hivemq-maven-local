package af;

import ak.VectorClock;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import v1.MessageQueueRemoveAllReplicateRequest;

public class MessageQueueRemoveAllReplicateRequestSerializer
        extends Serializer<MessageQueueRemoveAllReplicateRequest> {

    @Override
    public void write(Kryo kryo, Output output, MessageQueueRemoveAllReplicateRequest object) {
        output.writeString(object.getClientId());
        output.writeLong(object.getTimestamp());
        kryo.writeObject(output, object.getVectorClock());
    }

    @Override
    public MessageQueueRemoveAllReplicateRequest read(Kryo kryo, Input input, Class<MessageQueueRemoveAllReplicateRequest> type) {
        String clientId = input.readString();
        long timestamp = input.readLong();
        VectorClock vectorClock = kryo.readObject(input, VectorClock.class);
        return new MessageQueueRemoveAllReplicateRequest(clientId, timestamp, vectorClock);
    }
}
