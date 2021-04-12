package aw;

import av.InternalConfigurationService;
import av.Internals;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.util.HashMap;
import java.util.Map;

@Singleton
public class InternalConfigurationServiceImpl implements InternalConfigurationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(InternalConfigurationServiceImpl.class);
    private final Map<String, String> configs = Maps.newConcurrentMap();

    public String get(String key) {
        String str = this.configs.get(key);
        if (str != null) {
            return str;
        }
        return Internals.DEFAULT.get(key);
    }

    public void set(String key, String value) {
        LOGGER.debug("Setting internal configuration '{}' to '{}'", key, value);
        this.configs.put(key, value);
    }

    public Map<String, String> getConfigurations() {
        HashMap configurations = Maps.newHashMap();
        configurations.putAll(Internals.DEFAULT);
        configurations.putAll(this.configs);
        return ImmutableMap.copyOf(configurations);
    }

    public boolean getBoolean(String key) {
        String value = get(key);
        return Boolean.parseBoolean(value);
    }

    public double getDouble(String key) {
        try {
            String value = get(key);
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            String defaultValue = Internals.DEFAULT.get(key);
            Preconditions.checkState(defaultValue != null, "Illegal format for internal configuration " + key + " and no default value available");
            LOGGER.debug("Illegal format for internal configuration {} using default value {}", key, defaultValue);
            set(key, defaultValue);
            return Double.parseDouble(defaultValue);
        }
    }

    public int getInt(String key) {
        try {
            String value = get(key);
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            String defaultValue = Internals.DEFAULT.get(key);
            Preconditions.checkState(defaultValue != null, "Illegal format for internal configuration " + key + " and no default value available");
            LOGGER.debug("Illegal format for internal configuration {} using default value {}", key, defaultValue);
            set(key, defaultValue);
            return Integer.parseInt(defaultValue);
        }
    }

    public long getLong(String key) {
        try {
            String value = get(key);
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            String defaultValue = Internals.DEFAULT.get(key);
            Preconditions.checkState(defaultValue != null, "Illegal format for internal configuration " + key + " and no default value available");
            LOGGER.debug("Illegal format for internal configuration {} using default value {}", key, defaultValue);
            set(key, defaultValue);
            return Long.parseLong(defaultValue);
        }
    }
}
