package ac;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.hivemq.spi.message.QoS;
import com.hivemq.spi.message.Topic;

public class TopicSerializer extends Serializer<Topic> {

    @Override
    public void write(Kryo kryo, Output output, Topic object) {
        output.writeString(object.getTopic());
        kryo.writeObjectOrNull(output, object.getQoS(), QoS.class);
    }

    @Override
    public Topic read(Kryo kryo, Input input, Class<Topic> type) {
        String topic = input.readString();
        QoS qoS = kryo.readObjectOrNull(input, QoS.class);
        return new Topic(topic, qoS);
    }
}
