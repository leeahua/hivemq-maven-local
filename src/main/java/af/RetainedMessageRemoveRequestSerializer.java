package af;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import x1.RetainedMessageRemoveRequest;

public class RetainedMessageRemoveRequestSerializer
        extends Serializer<RetainedMessageRemoveRequest> {

    @Override
    public void write(Kryo kryo, Output output, RetainedMessageRemoveRequest object) {
        output.writeLong(object.getTimestamp());
        output.writeString(object.getTopic());
    }

    @Override
    public RetainedMessageRemoveRequest read(Kryo kryo, Input input, Class<RetainedMessageRemoveRequest> type) {
        long timestamp = input.readLong();
        String topic = input.readString();
        return new RetainedMessageRemoveRequest(timestamp, topic);
    }
}
