package as;

import au.ConfigFile;
import com.hivemq.spi.config.SystemInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class ConfigFileProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigFileProvider.class);

    public static ConfigFile get(SystemInformation systemInformation) {
        File configFolder = systemInformation.getConfigFolder();
        boolean validConfigFolder = validConfigFolder(configFolder);
        File configFile = new File(configFolder, "config.xml");
        boolean validConfigFile = false;
        if (validConfigFolder) {
            validConfigFile = validConfigFile(configFile);
        }
        if (validConfigFolder && validConfigFile) {
            return new ConfigFile(configFile);
        }
        return new ConfigFile(null);
    }

    private static boolean validConfigFile(File configFile) {
        if (!configFile.exists()) {
            LOGGER.error("The configuration file {} does not exist. Using HiveMQ default config", configFile.getAbsolutePath());
            return false;
        }
        if (!configFile.isFile()) {
            LOGGER.error("The configuration file {} is not file. Using HiveMQ default config", configFile.getAbsolutePath());
            return false;
        }
        if (!configFile.canRead()) {
            LOGGER.error("The configuration file {} cannot be read by HiveMQ. Using HiveMQ default config", configFile.getAbsolutePath());
            return false;
        }
        if (!configFile.canWrite()) {
            LOGGER.warn("The configuration file {} is read only and cannot be written by HiveMQ.", configFile.getAbsolutePath());
        }
        return true;
    }

    private static boolean validConfigFolder(File configFolder) {
        if (!configFolder.exists()) {
            LOGGER.error("The configuration file folder {} does not exist. Using HiveMQ default config", configFolder.getAbsolutePath());
            return false;
        }
        if (!configFolder.isDirectory()) {
            LOGGER.error("The configuration file folder {} is not a folder. Using HiveMQ default config", configFolder.getAbsolutePath());
            return false;
        }
        if (!configFolder.canRead()) {
            LOGGER.error("The configuration file folder {} cannot be read by HiveMQ. Using HiveMQ default config", configFolder.getAbsolutePath());
            return false;
        }
        return true;
    }
}
