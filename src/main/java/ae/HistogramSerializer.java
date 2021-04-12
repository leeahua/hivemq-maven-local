package ae;

import com.codahale.metrics.Histogram;
import com.codahale.metrics.Snapshot;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import java.io.OutputStream;

public class HistogramSerializer extends Serializer<Histogram> {
    @Override
    public void write(Kryo kryo, Output output, Histogram object) {
        Snapshot snapshot = object.getSnapshot();
        output.writeLong(object.getCount());
        output.writeLong(snapshot.getMin());
        output.writeLong(snapshot.getMax());
        output.writeInt(snapshot.size());
        output.writeDouble(snapshot.get75thPercentile());
        output.writeDouble(snapshot.get95thPercentile());
        output.writeDouble(snapshot.get98thPercentile());
        output.writeDouble(snapshot.get99thPercentile());
        output.writeDouble(snapshot.get999thPercentile());
        output.writeDouble(snapshot.getMedian());
        output.writeDouble(snapshot.getMean());
        output.writeDouble(snapshot.getStdDev());
    }

    @Override
    public Histogram read(Kryo kryo, Input input, Class<Histogram> type) {
        long count = input.readLong();
        long min = input.readLong();
        long max = input.readLong();
        int size = input.readInt();
        double percentile75 = input.readDouble();
        double percentile95 = input.readDouble();
        double percentile98 = input.readDouble();
        double percentile99 = input.readDouble();
        double percentile999 = input.readDouble();
        double median = input.readDouble();
        double mean = input.readDouble();
        double stdDev = input.readDouble();
        InternalSnapshot snapshot = new InternalSnapshot(min, max, size, percentile75, percentile95, percentile98, percentile99, percentile999, median, mean, stdDev);
        return new InternalHistogram(snapshot, count);
    }

    private final class InternalSnapshot extends Snapshot {
        final long min;
        final long max;
        final int size;
        final double percentile75;
        final double percentile95;
        final double percentile98;
        final double percentile99;
        final double percentile999;
        final double median;
        final double mean;
        final double stdDev;

        public InternalSnapshot(long min,
                                long max,
                                int size,
                                double percentile75,
                                double percentile95,
                                double percentile98,
                                double percentile99,
                                double percentile999,
                                double median,
                                double mean,
                                double stdDev) {
            this.min = min;
            this.max = max;
            this.size = size;
            this.percentile75 = percentile75;
            this.percentile95 = percentile95;
            this.percentile98 = percentile98;
            this.percentile99 = percentile99;
            this.percentile999 = percentile999;
            this.median = median;
            this.mean = mean;
            this.stdDev = stdDev;
        }

        public double getMedian() {
            return this.median;
        }

        public double get75thPercentile() {
            return this.percentile75;
        }

        public double get95thPercentile() {
            return this.percentile95;
        }

        public double get98thPercentile() {
            return this.percentile98;
        }

        public double get99thPercentile() {
            return this.percentile99;
        }

        public double get999thPercentile() {
            return this.percentile999;
        }

        public double getValue(double paramDouble) {
            throw new UnsupportedOperationException("This histogram can not be modified.");
        }

        public long[] getValues() {
            throw new UnsupportedOperationException("This histogram can not be modified.");
        }

        public int size() {
            return this.size;
        }

        public long getMax() {
            return this.max;
        }

        public double getMean() {
            return this.mean;
        }

        public long getMin() {
            return this.min;
        }

        public double getStdDev() {
            return this.stdDev;
        }

        public void dump(OutputStream output) {
            throw new UnsupportedOperationException("This histogram can not be modified.");
        }
    }

    private final class InternalHistogram extends Histogram {
        private final InternalSnapshot snapshot;
        private final long count;

        public InternalHistogram(InternalSnapshot snapshot, long count) {
            super(null);
            this.snapshot = snapshot;
            this.count = count;
        }

        public void update(int paramInt) {
            throw new UnsupportedOperationException("This histogram can not be modified.");
        }

        public void update(long paramLong) {
            throw new UnsupportedOperationException("This histogram can not be modified.");
        }

        public long getCount() {
            return this.count;
        }

        public Snapshot getSnapshot() {
            return this.snapshot;
        }
    }
}
