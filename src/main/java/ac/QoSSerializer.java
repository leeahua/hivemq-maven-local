package ac;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.hivemq.spi.message.QoS;

public class QoSSerializer extends Serializer<QoS> {

    @Override
    public void write(Kryo kryo, Output output, QoS object) {
        output.writeInt(object.getQosNumber());
    }

    @Override
    public QoS read(Kryo kryo, Input input, Class<QoS> type) {
        return QoS.valueOf(input.readInt());
    }
}
