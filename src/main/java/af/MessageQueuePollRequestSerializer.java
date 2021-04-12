package af;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import v1.MessageQueuePollRequest;

public class MessageQueuePollRequestSerializer
        extends Serializer<MessageQueuePollRequest> {

    @Override
    public void write(Kryo kryo, Output output, MessageQueuePollRequest object) {
        output.writeString(object.getClientId());
        output.writeString(object.getRequestId());
    }

    @Override
    public MessageQueuePollRequest read(Kryo kryo, Input input, Class<MessageQueuePollRequest> type) {
        String clientId = input.readString();
        String requestId = input.readString();
        return new MessageQueuePollRequest(clientId, requestId);
    }
}
