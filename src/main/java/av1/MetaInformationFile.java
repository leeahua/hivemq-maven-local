package av1;

import at1.Migrations;
import bd1.PersistenceFolders;
import com.hivemq.spi.annotations.NotNull;
import com.hivemq.spi.config.SystemInformation;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

public class MetaInformationFile {
    private static final Logger LOGGER = LoggerFactory.getLogger(MetaInformationFile.class);
    private static final Logger MIGRATIONS_LOGGER = LoggerFactory.getLogger(Migrations.LOGGER_NAME);
    public static final String FOLDER_NAME = "metadata";
    public static final String FILE_NAME = "versions.hmq";
    public static final MetadataSerializer SERIALIZER = new MetadataSerializer();

    @NotNull
    public static MetaInformation read(SystemInformation systemInformation) {
        File dataFolder = systemInformation.getDataFolder();
        MetaInformation metaInformation = new MetaInformation();
        if (!dataFolder.exists()) {
            return metaInformation;
        }
        metaInformation.setDataFolderExists(true);
        File persistenceFolder = new File(dataFolder, PersistenceFolders.FOLDER_NAME);
        if (!persistenceFolder.exists()) {
            return metaInformation;
        }
        metaInformation.setPersistenceFolderExists(true);
        File metadataFile = new File(dataFolder, MetaInformationFile.FOLDER_NAME + File.separator + MetaInformationFile.FILE_NAME);
        if (!metadataFile.exists()) {
            return metaInformation;
        }
        metaInformation.setMetadataFileExists(true);
        try {
            byte[] metadata = FileUtils.readFileToByteArray(metadataFile);
            MetaInformation information = SERIALIZER.read(metadata);
            MIGRATIONS_LOGGER.info("Read metadata file: {}", information);
            return information;
        } catch (IOException e) {
            MIGRATIONS_LOGGER.error("Not able to read metadata file", e);
            LOGGER.trace("Not able to read meta file", e);
            metaInformation.setMetadataFileExists(false);
        }
        return metaInformation;
    }

    public static void write(SystemInformation systemInformation, MetaInformation metaInformation) {
        File metadataFile = getMetadataFile(systemInformation);
        try {
            FileUtils.writeByteArrayToFile(metadataFile, SERIALIZER.write(metaInformation), false);
            MIGRATIONS_LOGGER.info("Write metadata file: {}", metaInformation);
        } catch (IOException e) {
            MIGRATIONS_LOGGER.error("Not able to write metadata file", e);
            LOGGER.error("Not able to write metadata file, please check your file and folder permissions");
            LOGGER.debug("Original exception", e);
        }
    }

    @NotNull
    private static File getMetadataFile(SystemInformation systemInformation) {
        return new File(systemInformation.getDataFolder(), MetaInformationFile.FOLDER_NAME + File.separator + MetaInformationFile.FILE_NAME);
    }
}
