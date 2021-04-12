package af;

import bz.RetainedMessage;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import x1.RetainedMessageAddRequest;

public class RetainedMessageAddRequestSerializer
        extends Serializer<RetainedMessageAddRequest> {

    @Override
    public void write(Kryo kryo, Output output, RetainedMessageAddRequest object) {
        output.writeLong(object.getTimestamp());
        output.writeString(object.getTopic());
        kryo.writeObject(output, object.getRetainedMessage());
    }

    @Override
    public RetainedMessageAddRequest read(Kryo kryo, Input input, Class<RetainedMessageAddRequest> type) {
        long timestamp = input.readLong();
        String topic = input.readString();
        RetainedMessage retainedMessage = kryo.readObject(input, RetainedMessage.class);
        return new RetainedMessageAddRequest(timestamp, topic, retainedMessage);
    }
}
