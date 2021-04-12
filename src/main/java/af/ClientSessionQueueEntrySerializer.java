package af;

import bc1.ClientSessionQueueEntry;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.hivemq.spi.message.QoS;

public class ClientSessionQueueEntrySerializer
        extends Serializer<ClientSessionQueueEntry> {

    @Override
    public void write(Kryo kryo, Output output, ClientSessionQueueEntry object) {
        output.writeLong(object.getSequence());
        output.writeLong(object.getTimestamp());
        output.writeString(object.getTopic());
        output.writeInt(object.getQoS().getQosNumber());
        output.writeInt(object.getPayload().length);
        output.writeBytes(object.getPayload());
        output.writeString(object.getClusterId());
    }

    @Override
    public ClientSessionQueueEntry read(Kryo kryo, Input input, Class<ClientSessionQueueEntry> type) {
        long sequence = input.readLong();
        long timestamp = input.readLong();
        String topic = input.readString();
        QoS qoS = QoS.valueOf(input.readInt());
        int payloadLength = input.readInt();
        byte[] payload = input.readBytes(payloadLength);
        String clusterId = input.readString();
        return new ClientSessionQueueEntry(sequence, timestamp, topic, qoS, payload, clusterId);
    }
}
