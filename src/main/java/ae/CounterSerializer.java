package ae;

import com.codahale.metrics.Counter;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

public class CounterSerializer extends Serializer<Counter> {

    @Override
    public void write(Kryo kryo, Output output, Counter object) {
        output.writeLong(object.getCount());
    }

    @Override
    public Counter read(Kryo kryo, Input input, Class<Counter> type) {
        long count = input.readLong();
        return new InternalCounter(count);
    }

    private static class InternalCounter extends Counter {
        private final long count;

        private InternalCounter(long count) {
            this.count = count;
        }

        public void inc() {
            throw new UnsupportedOperationException("This counter can not be modified.");
        }

        public void inc(long paramLong) {
            throw new UnsupportedOperationException("This counter can not be modified.");
        }

        public void dec() {
            throw new UnsupportedOperationException("This counter can not be modified.");
        }

        public void dec(long paramLong) {
            throw new UnsupportedOperationException("This counter can not be modified.");
        }

        public long getCount() {
            return this.count;
        }
    }
}
