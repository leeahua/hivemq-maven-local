package ar;

import aq.SystemInformationKeys;
import cb1.ManifestUtils;
import com.google.common.io.Files;
import com.hivemq.HiveMQServer;
import com.hivemq.spi.config.SystemInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

public class HiveMQSystemInformation implements SystemInformation {
    private static final Logger LOGGER = LoggerFactory.getLogger(HiveMQSystemInformation.class);
    private File homeFolder;
    private File pluginFolder;
    private File configFolder;
    private File logFolder;
    private File licenseFolder;
    private File dataFolder;
    private String version;
    private final boolean useCurrent;

    public HiveMQSystemInformation() {
        this.useCurrent = false;
        initVersion();
        initFolders();
    }

    public HiveMQSystemInformation(boolean useCurrent) {
        this.useCurrent = useCurrent;
        initVersion();
        initFolders();
    }

    private void initFolders() {
        initHomeFolder();
        this.pluginFolder = findAbsoluteAndRelative(SystemInformationKeys.PLUGIN, "plugins");
        this.configFolder = findAbsoluteAndRelative(SystemInformationKeys.CONFIG, "conf");
        this.logFolder = findAbsoluteAndRelative(SystemInformationKeys.LOG, "log");
        this.licenseFolder = findAbsoluteAndRelative(SystemInformationKeys.LICENSE, "license");
        this.dataFolder = findAbsoluteAndRelative(SystemInformationKeys.DATA, "data");
    }

    private void initVersion() {
        this.version = ManifestUtils.attribute(HiveMQServer.class, "Implementation-Version");
        if ((this.version == null) || (this.version.length() < 1)) {
            this.version = "Development Snapshot";
        }
        LOGGER.info("HiveMQ version: {}", this.version);
    }

    public String getHiveMQVersion() {
        return this.version;
    }

    public File getHiveMQHomeFolder() {
        return this.homeFolder;
    }

    public File getPluginFolder() {
        return this.pluginFolder;
    }

    public File getConfigFolder() {
        return this.configFolder;
    }

    public File getLogFolder() {
        return this.logFolder;
    }

    public File getLicenseFolder() {
        return this.licenseFolder;
    }

    public File getDataFolder() {
        return this.dataFolder;
    }

    private File findAbsoluteAndRelative(String location) {
        File file = new File(location);
        if (file.isAbsolute()) {
            return file;
        }
        return new File(getHiveMQHomeFolder(), location);
    }

    private File findAbsoluteAndRelative(String configKey, String defaultLocation) {
        String config = System.getProperty(configKey);
        if (config != null) {
            return findAbsoluteAndRelative(config);
        }
        return findAbsoluteAndRelative(defaultLocation);
    }

    private void initHomeFolder() {
        String str = System.getProperty(SystemInformationKeys.HOME);
        if (str != null) {
            this.homeFolder = findAbsoluteAndRelative(str);
            LOGGER.info("HiveMQ home directory: {}", this.homeFolder.getAbsolutePath());
            System.setProperty(SystemInformationKeys.HOME, this.homeFolder.getAbsolutePath());
            return;
        }
        if (this.useCurrent) {
            initHomeFolderUseCurrent();
        } else {
            initHomeFolderUseTemp();
        }
    }

    private void initHomeFolderUseTemp() {
        File temp = Files.createTempDir();
        temp.deleteOnExit();
        LOGGER.warn("No hivemq.home property was set. Using a temporary directory ({}) HiveMQ will behave unexpectedly!", temp.getAbsolutePath());
        this.homeFolder = temp;
    }

    private void initHomeFolderUseCurrent() {
        File folder = currentFolder();
        LOGGER.warn("No {} property was set. Using {}", SystemInformationKeys.HOME, folder.getAbsolutePath());
        this.homeFolder = folder;
    }

    private File currentFolder() {
        try {
            String jarPath = URLDecoder.decode(HiveMQServer.class.getProtectionDomain().getCodeSource().getLocation().getPath(), "UTF-8");
            String path = jarPath.substring(0, jarPath.lastIndexOf('/') + 1);
            return new File(path);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("UTF-8 encoding is not supported", e);
        }
    }
}
