package af;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.google.common.collect.ImmutableMap;
import v1.MessageQueueReplication;
import v1.MessageQueueReplicateRequest;

public class MessageQueueReplicateRequestSerializer
        extends Serializer<MessageQueueReplicateRequest> {

    @Override
    public void write(Kryo kryo, Output output, MessageQueueReplicateRequest object) {
        kryo.writeClassAndObject(output, object.getReplications());
    }

    @Override
    public MessageQueueReplicateRequest read(Kryo kryo, Input input, Class<MessageQueueReplicateRequest> type) {
        ImmutableMap<String, MessageQueueReplication> replications = (ImmutableMap<String, MessageQueueReplication>) kryo.readClassAndObject(input);
        return new MessageQueueReplicateRequest(replications);
    }
}
