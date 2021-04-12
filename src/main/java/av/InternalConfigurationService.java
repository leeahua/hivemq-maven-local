package av;

import java.util.Map;

public interface InternalConfigurationService {
    String get(String key);

    Map<String, String> getConfigurations();

    void set(String key, String value);

    boolean getBoolean(String key);

    double getDouble(String key);

    int getInt(String key);

    long getLong(String key);
}
