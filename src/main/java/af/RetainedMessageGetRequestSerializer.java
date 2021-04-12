package af;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import x1.RetainedMessageGetRequest;

public class RetainedMessageGetRequestSerializer
        extends Serializer<RetainedMessageGetRequest> {

    @Override
    public void write(Kryo kryo, Output output, RetainedMessageGetRequest object) {
        output.writeString(object.getTopic());
    }

    @Override
    public RetainedMessageGetRequest read(Kryo kryo, Input input, Class<RetainedMessageGetRequest> type) {
        String topic = input.readString();
        return new RetainedMessageGetRequest(topic);
    }
}
