package az;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.Map;

public class SystemPropertiesInformation extends AbstractInformation {
    public String get() {
        StringBuilder builder = new StringBuilder();
        RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        Map<String, String> systemProperties = runtimeMXBean.getSystemProperties();
        systemProperties.forEach((key, value) ->
                addInformation(builder, key, value)
        );
        return builder.toString();
    }
}
