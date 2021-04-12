package af;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import v1.MessageQueueDrainedAckRequest;

public class MessageQueueDrainedAckRequestSerializer
        extends Serializer<MessageQueueDrainedAckRequest> {

    @Override
    public void write(Kryo kryo, Output output, MessageQueueDrainedAckRequest object) {
        output.writeString(object.getClientId());
    }

    @Override
    public MessageQueueDrainedAckRequest read(Kryo kryo, Input input, Class<MessageQueueDrainedAckRequest> type) {
        String clientId = input.readString();
        return new MessageQueueDrainedAckRequest(clientId);
    }
}
