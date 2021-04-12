package ac;

import bu.InternalPublish;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.hivemq.spi.message.QoS;

public class InternalPublishSerializer extends Serializer<InternalPublish> {

    @Override
    public void write(Kryo kryo, Output output, InternalPublish object) {
        output.writeLong(object.getSequence());
        output.writeLong(object.getTimestamp());
        output.writeString(object.getClusterId());
        output.writeInt(object.getMessageId());
        output.writeBoolean(object.isRetain());
        output.writeBoolean(object.isDuplicateDelivery());
        output.writeInt(object.getQoS().getQosNumber());
        output.writeString(object.getTopic());
        output.writeInt(object.getPayload().length);
        output.write(object.getPayload());
    }

    @Override
    public InternalPublish read(Kryo kryo, Input input, Class<InternalPublish> type) {
        long sequence = input.readLong();
        long timestamp = input.readLong();
        String clusterId = input.readString();
        InternalPublish publish = new InternalPublish(sequence, timestamp, clusterId);
        publish.setMessageId(input.readInt());
        publish.setRetain(input.readBoolean());
        publish.setDuplicateDelivery(input.readBoolean());
        publish.setQoS(QoS.valueOf(input.readInt()));
        publish.setTopic(input.readString());
        int payloadLength = input.readInt();
        byte[] payload = input.readBytes(payloadLength);
        publish.setPayload(payload);
        return publish;
    }
}
