package ac;

import bz.RetainedMessage;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.hivemq.spi.message.QoS;

public class RetainedMessageSerializer extends Serializer<RetainedMessage> {

    @Override
    public void write(Kryo kryo, Output output, RetainedMessage object) {
        output.writeInt(object.getQoS().getQosNumber());
        output.writeInt(object.getMessage().length);
        output.write(object.getMessage());
    }

    @Override
    public RetainedMessage read(Kryo kryo, Input input, Class<RetainedMessage> type) {
        QoS qoS = QoS.valueOf(input.readInt());
        int messageLength = input.readInt();
        byte[] message = input.readBytes(messageLength);
        return new RetainedMessage(message, qoS);
    }
}
