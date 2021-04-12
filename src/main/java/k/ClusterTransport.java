package k;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ClusterTransport {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterTransport.class);
    private int bindPort;
    private String bindAddress;

    public ClusterTransport(int bindPort, String bindAddress) {
        this.bindPort = bindPort;
        this.bindAddress = bindAddress;
    }

    public int getBindPort() {
        return this.bindPort;
    }

    public String getBindAddress() {
        return this.bindAddress;
    }

    public void setBindPort(int bindPort) {
        LOGGER.debug("Set bind-port {} for cluster {} transport", bindPort, getType());
        this.bindPort = bindPort;
    }

    public void setBindAddress(String bindAddress) {
        LOGGER.debug("Set bind-address {} for cluster {} transport", bindAddress, getType());
        this.bindAddress = bindAddress;
    }

    public abstract Type getType();

    public enum Type {
        TCP("TCP", 1),
        UDP("UDP", 2);
        private final String name;
        private final int value;

        Type(String name, int value) {
            this.name = name;
            this.value = value;
        }

        public String toString() {
            return this.name;
        }

        public int getValue() {
            return value;
        }

        public static Type a(int value) {
            for (Type type : values()) {
                if (type.getValue() == value) {
                    return type;
                }
            }
            throw new IllegalArgumentException("No type found for the given value : " + value + ".");
        }
    }
}
