package ae;

import com.codahale.metrics.Gauge;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

public class GaugeSerializer extends Serializer<Gauge<Number>> {

    @Override
    public void write(Kryo kryo, Output output, Gauge<Number> object) {
        output.writeDouble(object.getValue().doubleValue());
    }

    @Override
    public Gauge<Number> read(Kryo kryo, Input input, Class<Gauge<Number>> type) {
        double value = input.readDouble();
        return new InternalGauge(value);
    }

    private static class InternalGauge implements Gauge<Number> {
        private final Number value;

        private InternalGauge(Number value) {
            this.value = value;
        }

        @Override
        public Number getValue() {
            return value;
        }
    }
}
