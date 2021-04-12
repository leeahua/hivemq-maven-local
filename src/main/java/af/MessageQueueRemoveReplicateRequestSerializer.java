package af;

import ak.VectorClock;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import v1.MessageQueueRemoveReplicateRequest;

public class MessageQueueRemoveReplicateRequestSerializer
        extends Serializer<MessageQueueRemoveReplicateRequest> {

    @Override
    public void write(Kryo kryo, Output output, MessageQueueRemoveReplicateRequest object) {
        output.writeString(object.getClientId());
        output.writeString(object.getEntryId());
        output.writeLong(object.getEntryTimestamp());
        output.writeLong(object.getTimestamp());
        kryo.writeObject(output, object.getVectorClock());
    }

    @Override
    public MessageQueueRemoveReplicateRequest read(Kryo kryo, Input input, Class<MessageQueueRemoveReplicateRequest> type) {
        String clientId = input.readString();
        String entryId = input.readString();
        long entryTimestamp = input.readLong();
        long timestamp = input.readLong();
        VectorClock vectorClock = kryo.readObject(input, VectorClock.class);
        return new MessageQueueRemoveReplicateRequest(clientId, entryTimestamp, entryId, timestamp, vectorClock);
    }
}
