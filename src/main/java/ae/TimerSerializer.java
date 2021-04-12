package ae;

import com.codahale.metrics.Snapshot;
import com.codahale.metrics.Timer;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

public class TimerSerializer extends Serializer<Timer> {

    @Override
    public void write(Kryo kryo, Output output, Timer object) {
        output.writeDouble(object.getMeanRate());
        output.writeDouble(object.getOneMinuteRate());
        output.writeDouble(object.getFiveMinuteRate());
        output.writeDouble(object.getFifteenMinuteRate());
        output.writeLong(object.getCount());
    }

    @Override
    public Timer read(Kryo kryo, Input input, Class<Timer> type) {
        double meanRate = input.readDouble();
        double oneMinuteRate = input.readDouble();
        double fiveMinuteRate = input.readDouble();
        double fifteenMinuteRate = input.readDouble();
        long count = input.readLong();
        return new InternalTimer(meanRate, oneMinuteRate, fiveMinuteRate, fifteenMinuteRate, count);
    }

    private static class InternalTimer extends Timer {
        private final double meanRate;
        private final double oneMinuteRate;
        private final double fiveMinuteRate;
        private final double fifteenMinuteRate;
        private final long count;

        private InternalTimer(double meanRate,
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

        public void update(long paramLong, TimeUnit paramTimeUnit) {
            throw new UnsupportedOperationException("This timer can not be modified.");
        }

        public <T> T time(Callable<T> paramCallable) {
            throw new UnsupportedOperationException("This timer can not be modified.");
        }

        public Timer.Context time() {
            throw new UnsupportedOperationException("This timer can not be modified.");
        }

        public Snapshot getSnapshot() {
            throw new UnsupportedOperationException("This timer can not be modified.");
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
