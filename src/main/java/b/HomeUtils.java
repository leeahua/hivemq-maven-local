package b;

import aq.SystemInformationKeys;
import com.hivemq.HiveMQServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

public class HomeUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(HomeUtils.class);

    public static void setHomePath() {
        if (System.getProperty(SystemInformationKeys.HOME) == null) {
            File homeFolder = getHomeFolder();
            LOGGER.warn("No {} property was set. Using {}", SystemInformationKeys.HOME, homeFolder.getAbsolutePath());
            System.setProperty(SystemInformationKeys.HOME, homeFolder.getAbsolutePath());
        }
        LOGGER.info("HiveMQ home directory: {}", System.getProperty(SystemInformationKeys.HOME));
    }

    private static File getHomeFolder() {
        try {
            String jarPath = URLDecoder.decode(HiveMQServer.class.getProtectionDomain().getCodeSource().getLocation().getPath(), "UTF-8");
            String homePath = jarPath.substring(0, jarPath.lastIndexOf('/') + 1);
            return new File(homePath);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("UTF-8 encoding is not supported", e);
        }
    }
}
