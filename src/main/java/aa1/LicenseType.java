package aa1;

public enum LicenseType {
    NONE("none"),
    CONNECTION_LIMITED("connection-limited");

    private final String name;

    LicenseType(String name) {
        this.name = name;
    }

    public String toString() {
        return this.name;
    }
}
