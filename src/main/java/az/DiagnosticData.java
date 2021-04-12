package az;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;

import javax.inject.Inject;

public class DiagnosticData {
    private static final String BANNER = "   __ ___          __  _______     ___  _                         __  _       \n  / // (_)  _____ /  |/  / __ \\   / _ \\(_)__ ____ ____  ___  ___ / /_(_)______\n / _  / / |/ / -_) /|_/ / /_/ /  / // / / _ `/ _ `/ _ \\/ _ \\(_-</ __/ / __(_-<\n/_//_/_/|___/\\__/_/  /_/\\___\\_\\ /____/_/\\_,_/\\_, /_//_/\\___/___/\\__/_/\\__/___/\n                                            /___/                             \n";
    private static final String SECTION_HEADLINE = "##############################\n";
    private final SystemPropertiesInformation systemPropertiesInformation;
    private final HiveMQLicenseInformation hiveMQLicenseInformation;
    private final HiveMQInformation hiveMQInformation;
    private final SystemInformation systemInformation;
    private final NetworkInterfaceInformation networkInterfaceInformation;
    private final PluginsInformation pluginsInformation;

    @Inject
    DiagnosticData(SystemPropertiesInformation systemPropertiesInformation,
                   HiveMQLicenseInformation hiveMQLicenseInformation,
                   HiveMQInformation hiveMQInformation,
                   SystemInformation systemInformation,
                   NetworkInterfaceInformation networkInterfaceInformation,
                   PluginsInformation pluginsInformation) {
        this.systemPropertiesInformation = systemPropertiesInformation;
        this.hiveMQLicenseInformation = hiveMQLicenseInformation;
        this.hiveMQInformation = hiveMQInformation;
        this.systemInformation = systemInformation;
        this.networkInterfaceInformation = networkInterfaceInformation;
        this.pluginsInformation = pluginsInformation;
    }

    public String getContent() {
        StringBuilder builder = new StringBuilder();
        builder.append(BANNER);
        builder.append("\n");
        builder.append(String.format("Generated at %s \n", DateTime.now().toString()));
        builder.append("Please send this file along with any other files in the 'diagnostic' folder to support@hivemq.com.\n\n");
        builder.append(buildSection("HiveMQ Information"));
        builder.append(this.hiveMQInformation.get());
        builder.append(buildSection("HiveMQ License Information"));
        builder.append(this.hiveMQLicenseInformation.get());
        builder.append(buildSection("Java System Properties"));
        builder.append(this.systemPropertiesInformation.get());
        builder.append(buildSection("System Information"));
        builder.append(this.systemInformation.get());
        builder.append(buildSection("Network Interfaces"));
        builder.append(this.networkInterfaceInformation.get());
        builder.append(buildSection("Plugin Information"));
        builder.append(this.pluginsInformation.get());
        return builder.toString();
    }

    private String buildSection(String name) {
        StringBuilder builder = new StringBuilder();
        builder.append("\n");
        builder.append(SECTION_HEADLINE);
        builder.append("#");
        builder.append(StringUtils.center(name, SECTION_HEADLINE.length() - 3));
        builder.append("#\n");
        builder.append(SECTION_HEADLINE);
        builder.append("\n");
        return builder.toString();
    }
}
