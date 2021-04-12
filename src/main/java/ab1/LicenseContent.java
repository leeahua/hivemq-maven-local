package ab1;

import aa1.LicenseType;

import java.util.Properties;

public class LicenseContent {
    private final LicenseType type;
    private final Properties properties;

    public LicenseContent(LicenseType type, Properties properties) {
        this.type = type;
        this.properties = properties;
    }

    public LicenseType getType() {
        return type;
    }

    public Properties getProperties() {
        return properties;
    }
}
