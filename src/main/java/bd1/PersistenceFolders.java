package bd1;

import com.hivemq.spi.config.SystemInformation;
import d.CacheScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.File;

@CacheScoped
public class PersistenceFolders {
    public static final String FOLDER_NAME = "persistence";
    private static final Logger LOGGER = LoggerFactory.getLogger(PersistenceFolders.class);
    private final SystemInformation systemInformation;

    @Inject
    PersistenceFolders(SystemInformation systemInformation) {
        this.systemInformation = systemInformation;
    }

    public File root() {
        File dataFolder = this.systemInformation.getDataFolder();
        File persistenceFolder = new File(dataFolder, "persistence");
        if (!persistenceFolder.exists()) {
            LOGGER.debug("Folder {} does not exist, trying to create it", persistenceFolder.getAbsolutePath());
            boolean created = persistenceFolder.mkdirs();
            if (created) {
                LOGGER.debug("Created folder {}", dataFolder.getAbsolutePath());
            }
        }
        return persistenceFolder;
    }

    public File create(String parentFolderName, String childFolderName) {
        File folder = new File(root(), parentFolderName + File.separator + childFolderName);
        if (!folder.exists()) {
            LOGGER.debug("Folder {} does not exist, trying to create it", folder.getAbsolutePath());
            folder.mkdirs();
        }
        return folder;
    }
}
