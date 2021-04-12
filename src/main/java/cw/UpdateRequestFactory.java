package cw;

import cl.PluginInformation;
import cl.PluginInformationStore;
import com.hivemq.spi.config.SystemInformation;
import com.hivemq.update.entity.UpdateRequest;

import javax.inject.Inject;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class UpdateRequestFactory {
    private final PluginInformationStore pluginInformationStore;
    private final SystemInformation systemInformation;

    @Inject
    public UpdateRequestFactory(
            PluginInformationStore pluginInformationStore,
            SystemInformation systemInformation) {
        this.pluginInformationStore = pluginInformationStore;
        this.systemInformation = systemInformation;
    }

    public UpdateRequest create(String id) {
        UpdateRequest request = new UpdateRequest();
        request.setHiveMQInformation(createHiveMQInformation(id));
        request.setPlugins(createPluginInformation());
        request.setSystemInformation(createSystemInformation());
        return request;
    }

    private UpdateRequest.HiveMQRequest createHiveMQInformation(String id) {
        UpdateRequest.HiveMQRequest hiveMQRequest = new UpdateRequest.HiveMQRequest();
        hiveMQRequest.setId(id);
        hiveMQRequest.setVersion(this.systemInformation.getHiveMQVersion());
        return hiveMQRequest;
    }

    private List<UpdateRequest.Plugin> createPluginInformation() {
        Set<PluginInformation> pluginInformations = this.pluginInformationStore.getPluginInformations();
        return pluginInformations.stream()
                .map(pluginInformation -> {
                    UpdateRequest.Plugin plugin = new UpdateRequest.Plugin();
                    plugin.setName(pluginInformation.getName());
                    plugin.setVersion(pluginInformation.getVersion());
                    return plugin;
                })
                .collect(Collectors.toList());
    }

    private UpdateRequest.SystemInformation createSystemInformation() {
        UpdateRequest.SystemInformation systemInformation = new UpdateRequest.SystemInformation();
        systemInformation.setJavaVendor(System.getProperty("java.vendor"));
        systemInformation.setJavaVersion(System.getProperty("java.version"));
        systemInformation.setOsArch(System.getProperty("os.arch"));
        systemInformation.setOsName(System.getProperty("os.name"));
        systemInformation.setOsVersion(System.getProperty("os.version"));
        return systemInformation;
    }
}
