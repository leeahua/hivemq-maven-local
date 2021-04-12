package af;

import ak.VectorClock;
import bc1.ClientSessionQueueEntry;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.google.common.collect.ImmutableSet;
import v1.MessageQueueReplication;

public class MessageQueueReplicationSerializer
        extends Serializer<MessageQueueReplication> {

    @Override
    public void write(Kryo kryo, Output output, MessageQueueReplication object) {
        output.writeLong(object.getTimestamp());
        kryo.writeObject(output, object.getVectorClock());
        kryo.writeClassAndObject(output, object.getEntries());
    }

    @Override
    public MessageQueueReplication read(Kryo kryo, Input input, Class<MessageQueueReplication> type) {
        long timestamp = input.readLong();
        VectorClock vectorClock = kryo.readObject(input, VectorClock.class);
        ImmutableSet<ClientSessionQueueEntry> entries = (ImmutableSet<ClientSessionQueueEntry>) kryo.readClassAndObject(input);
        return new MessageQueueReplication(vectorClock, timestamp, entries);
    }
}
