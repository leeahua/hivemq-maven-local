package ae;

import com.codahale.metrics.Meter;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

public class MeterSerializer extends Serializer<Meter> {

    @Override
    public void write(Kryo kryo, Output output, Meter object) {
        output.writeDouble(object.getMeanRate());
        output.writeDouble(object.getOneMinuteRate());
        output.writeDouble(object.getFiveMinuteRate());
        output.writeDouble(object.getFifteenMinuteRate());
        output.writeLong(object.getCount());
    }

    @Override
    public Meter read(Kryo kryo, Input input, Class<Meter> type) {
        double meanRate = input.readDouble();
        double oneMinuteRate = input.readDouble();
        double fiveMinuteRate = input.readDouble();
        double fifteenMinuteRate = input.readDouble();
        long count = input.readLong();
        return new InternalMeter(meanRate, oneMinuteRate, fiveMinuteRate, fifteenMinuteRate, count);

    }

    private static class InternalMeter
            extends Meter {
        private final double meanRate;
        private final double oneMinuteRate;
        private final double fiveMinuteRate;
        private final double fifteenMinuteRate;
        private final long count;

        private InternalMeter(double meanRate,
                              double oneMinuteRate,
                              double fiveMinuteRate,
                              double fifteenMinuteRate,
                              long count) {
            this.meanRate = meanRate;
            this.oneMinuteRate = oneMinuteRate;
            this.fiveMinuteRate = fiveMinuteRate;
            this.fifteenMinuteRate = fifteenMinuteRate;
            this.count = count;
        }

        public void mark() {
            throw new UnsupportedOperationException("This mater can not be modified.");
        }

        public void mark(long paramLong) {
            throw new UnsupportedOperationException("This mater can not be modified.");
        }

        public long getCount() {
            return this.count;
        }

        public double getFifteenMinuteRate() {
            return this.fifteenMinuteRate;
        }

        public double getFiveMinuteRate() {
            return this.fiveMinuteRate;
        }

        public double getMeanRate() {
            return this.meanRate;
        }

        public double getOneMinuteRate() {
            return this.oneMinuteRate;
        }
    }
}
