package az;

import cl.PluginInformation;
import cl.PluginInformationStore;
import com.google.common.base.Joiner;
import com.hivemq.spi.callback.Callback;
import com.hivemq.spi.callback.registry.CallbackRegistry;
import org.apache.commons.lang3.exception.ExceptionUtils;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class PluginsInformation extends AbstractInformation {
    private final PluginInformationStore pluginInformationStore;
    private final CallbackRegistry callbackRegistry;

    @Inject
    PluginsInformation(PluginInformationStore pluginInformationStore,
                       CallbackRegistry callbackRegistry) {
        this.pluginInformationStore = pluginInformationStore;
        this.callbackRegistry = callbackRegistry;
    }

    public String get() {
        try {
            StringBuilder builder = new StringBuilder();
            Set<PluginInformation> pluginInformations = this.pluginInformationStore.getPluginInformations();
            if (pluginInformations.isEmpty()) {
                builder.append("No plugins installed\n");
            } else {
                builder.append(String.format("%s plugins installed\n", pluginInformations.size()));
            }
            builder.append("\n");
            pluginInformations.forEach(pluginInformation -> {
                Optional<String> mayName = Optional.ofNullable(pluginInformation.getName());
                builder.append(String.format("[%s]\n", mayName.orElse("Unknown Plugin")));
                addInformation(builder, "Version", pluginInformation.getVersion());
                addInformation(builder, "Author", pluginInformation.getAuthor());
                addInformation(builder, "Description", pluginInformation.getDescription());
                builder.append("\n");
            });
            Set<Class<? extends Callback>> callbackClasses = this.callbackRegistry.getAllRegisteredCallbackClasses();
            if (!callbackClasses.isEmpty()) {
                builder.append("[Registered Callbacks]\n");
                callbackClasses.forEach(callbackClass -> {
                    List<? extends Callback> callbacks = this.callbackRegistry.getCallbacks(callbackClass);
                    addInformation(builder, callbackClass.getSimpleName(), Joiner.on(",").join(callbacks));
                });
            } else {
                builder.append("No registered callbacks found\n");
            }
            return builder.toString();
        } catch (Exception e) {
            return "Could not get Plugin Information. Exception: " + ExceptionUtils.getStackTrace(e);
        }
    }
}
