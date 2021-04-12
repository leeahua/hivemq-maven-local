package af;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import y1.MetricRequest;

public class MetricRequestSerializer extends Serializer<MetricRequest> {

    @Override
    public void write(Kryo kryo, Output output, MetricRequest object) {
        output.writeString(object.getMetricName());
        kryo.writeObject(output, object.getMetricClass());
    }

    @Override
    public MetricRequest read(Kryo kryo, Input input, Class<MetricRequest> type) {
        String metricName = input.readString();
        Class metricClass = kryo.readObject(input, Class.class);
        return new MetricRequest(metricName, metricClass);
    }
}
