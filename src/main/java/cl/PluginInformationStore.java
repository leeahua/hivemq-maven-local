package cl;

import com.google.common.collect.ImmutableSet;
import com.hivemq.spi.PluginModule;
import com.hivemq.spi.plugin.meta.Information;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.Set;

@Singleton
public class PluginInformationStore {
    private static final Logger LOGGER = LoggerFactory.getLogger(PluginInformationStore.class);
    private Set<PluginInformation> pluginInformations;
    private final List<PluginModule> pluginModules;

    @Inject
    PluginInformationStore(List<PluginModule> pluginModules) {
        this.pluginModules = pluginModules;
    }

    @PostConstruct
    void init() {
        LOGGER.debug("Creating PluginInformationStore");
        ImmutableSet.Builder<PluginInformation> builder = ImmutableSet.builder();
        this.pluginModules.forEach(pluginModule -> {
            Information information = pluginModule.getClass().getAnnotation(Information.class);
            if (information != null) {
                PluginInformation pluginInformation = createPluginInformation(information);
                LOGGER.trace("Adding {}", pluginInformation);
                builder.add(pluginInformation);
                return;
            }
            LOGGER.warn("A Plugin doesn't have plugin information.");
            builder.add(new PluginInformation("unknown", pluginModule.getClass().getName(), "", "unknown"));
        });
        this.pluginInformations = builder.build();
    }

    public Set<PluginInformation> getPluginInformations() {
        return this.pluginInformations;
    }


    private PluginInformation createPluginInformation(Information information) {
        return new PluginInformation(information.version(),
                information.name(),
                information.description(),
                information.author());
    }
}
