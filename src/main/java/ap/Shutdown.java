package ap;

import com.hivemq.spi.annotations.NotNull;

public abstract class Shutdown extends Thread {
    @NotNull
    public abstract String name();

    @NotNull
    public abstract Priority priority();

    public abstract boolean isAsync();

    public abstract void run();

    public enum Priority {
        FIRST(Integer.MAX_VALUE),
        CRITICAL(1000000),
        VERY_HIGH(500000),
        HIGH(100000),
        MEDIUM(50000),
        LOW(10000),
        VERY_LOW(5000),
        DOES_NOT_MATTER(Integer.MIN_VALUE);

        private final int value;

        Priority(int value) {
            this.value = value;
        }

        public int getValue() {
            return this.value;
        }

        public String toString() {
            return name() + " (" + getValue() + ")";
        }
    }
}
