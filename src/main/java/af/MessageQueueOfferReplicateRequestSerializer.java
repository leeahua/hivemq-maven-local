package af;

import ak.VectorClock;
import bu.InternalPublish;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import v1.MessageQueueOfferReplicateRequest;

public class MessageQueueOfferReplicateRequestSerializer
        extends Serializer<MessageQueueOfferReplicateRequest> {

    @Override
    public void write(Kryo kryo, Output output, MessageQueueOfferReplicateRequest object) {
        output.writeString(object.getClientId());
        output.writeLong(object.getTimestamp());
        kryo.writeObjectOrNull(output, object.getPublish(), InternalPublish.class);
        kryo.writeObject(output, object.getVectorClock());
    }

    @Override
    public MessageQueueOfferReplicateRequest read(Kryo kryo, Input input, Class<MessageQueueOfferReplicateRequest> type) {
        String clientId = input.readString();
        long timestamp = input.readLong();
        InternalPublish publish = kryo.readObjectOrNull(input, InternalPublish.class);
        VectorClock vectorClock = kryo.readObject(input, VectorClock.class);
        return new MessageQueueOfferReplicateRequest(clientId, timestamp, vectorClock, publish);
    }
}
