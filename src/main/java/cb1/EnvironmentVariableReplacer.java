package cb1;

import com.google.inject.Singleton;
import com.hivemq.spi.annotations.Nullable;
import com.hivemq.spi.exceptions.UnrecoverableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Singleton
public class EnvironmentVariableReplacer {
    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentVariableReplacer.class);

    @Nullable
    public String getEnvironmentVariable(String key) {
        String value = System.getProperty(key);
        if (value != null) {
            return value;
        }
        return System.getenv(key);
    }

    public String replace(String content) {
        StringBuffer contentBuffer = new StringBuffer();
        Matcher placeholderMatcher = Pattern.compile("\\$\\{(.*?)\\}").matcher(content);
        while (placeholderMatcher.find()) {
            if (placeholderMatcher.groupCount() < 1) {
                LOGGER.warn("Found unexpected enviroment variable placeholder in config.xml");
                placeholderMatcher.appendReplacement(contentBuffer, "");
            } else {
                String key = placeholderMatcher.group(1);
                String value = getEnvironmentVariable(key);
                if (value == null) {
                    LOGGER.error("Environment Variable {} for HiveMQ config.xml is not set.", key);
                    throw new UnrecoverableException(false);
                }
                placeholderMatcher.appendReplacement(contentBuffer, getReplacement(value));
            }
        }
        placeholderMatcher.appendTail(contentBuffer);
        return contentBuffer.toString();
    }

    private String getReplacement(String value) {
        return value.replace("\\", "\\\\").replace("$", "\\$");
    }
}
