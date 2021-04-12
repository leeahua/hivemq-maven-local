package k;

public abstract class ClusterDiscovery {
    public abstract Type getType();

    public enum Type {
        STATIC(1),
        MULTICAST(2),
        BROADCAST(3),
        PLUGIN(4);
        private final int value;

        Type(int paramInt) {
            this.value = paramInt;
        }

        public int value() {
            return this.value;
        }

        public static Type valueOf(int value) {
            for (Type type : values()) {
                if (type.value() == value) {
                    return type;
                }
            }
            throw new IllegalArgumentException("No type found for the given value : " + value + ".");
        }
    }
}
