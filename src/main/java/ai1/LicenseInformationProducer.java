package ai1;

import aa1.LicenseType;
import ab1.LicenseContent;
import ab1.LicenseContentReader;
import ac1.LicenseFileCorruptException;
import ad1.LicenseInformationFactory;
import ae1.BuildTimeProducer;
import ae1.LicenseInformation;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;

public class LicenseInformationProducer {
    private static final Logger LOGGER = LoggerFactory.getLogger(LicenseInformationProducer.class);
    private final LicenseContentReader licenseContentReader;
    private final LicenseInformationFactory licenseInformationFactory;
    private final BuildTimeProducer buildTimeProducer;

    @Inject
    public LicenseInformationProducer(LicenseContentReader licenseContentReader,
                                      LicenseInformationFactory licenseInformationFactory,
                                      BuildTimeProducer buildTimeProducer) {
        this.licenseContentReader = licenseContentReader;
        this.licenseInformationFactory = licenseInformationFactory;
        this.buildTimeProducer = buildTimeProducer;
    }

    public List<LicenseInformation> produce(List<File> licenseFiles) {
        List<LicenseInformation> licenseInformations = Lists.newArrayList();
        licenseFiles.forEach(licenseFile -> {
            if (!licenseFile.exists()) {
                LOGGER.debug("License file {} does not exist (anymore)", licenseFile.getName());
                return;
            }
            if (licenseFile.isDirectory() || !licenseFile.getName().endsWith(".lic")) {
                return;
            }
            if (!licenseFile.canRead()) {
                LOGGER.warn("License file {} is not readable, please check your file permissions", licenseFile.getName());
                return;
            }
            LicenseInformation licenseInformation = read(licenseFile);
            if (licenseInformation == null) {
                return;
            }
            if (LicenseType.CONNECTION_LIMITED.equals(licenseInformation.getLicenseType()) &&
                    licenseInformation.getValidityEnd().isBeforeNow()) {
                LOGGER.warn("License file {} is expired, please contact {}.",
                        licenseFile.getName(), licenseInformation.getVendorEmail(),
                        licenseInformation.getVendorWebsite());
                expiredLicenseFile(licenseFile);
                return;
            }
            if (LicenseType.CONNECTION_LIMITED.equals(licenseInformation.getLicenseType())) {
                if (licenseInformation.getSupportedUntil() == null) {
                    LOGGER.warn("Your license ({}) is not valid for this version of HiveMQ, please use a supported version or contact {} (Please provide your license file and the HiveMQ version you would like to use).",
                            licenseFile.getName(), licenseInformation.getVendorEmail(), licenseInformation.getVendorWebsite());
                    invalidLicenseFile(licenseFile);
                    return;
                }
                if (licenseInformation.getSupportedUntil().isBefore(this.buildTimeProducer.getBuildDate())) {
                    LOGGER.warn("License file {} only has a software & support subscription until {}, this version is not supported. Please contact sales@hivemq.com for a renewal of the software & support subscription or downgrade to a supported HiveMQ version for your license.",
                            licenseFile.getAbsolutePath(), licenseInformation.getSupportedUntil().toString());
                    expiredLicenseFile(licenseFile);
                    return;
                }
            }
            licenseInformations.add(licenseInformation);
        });
        return licenseInformations;
    }

    private LicenseInformation read(File licenseFile) {
        try {
            LicenseContent licenseContent = this.licenseContentReader.read(licenseFile);
            return this.licenseInformationFactory.create(licenseContent, licenseFile.getName());
        } catch (FileNotFoundException e) {
            LOGGER.debug("License file {} not found", licenseFile.getName());
        } catch (LicenseFileCorruptException e) {
            invalidLicenseFile(licenseFile);
            LOGGER.warn("License file {} is corrupt", licenseFile.getName());
        }
        return null;
    }

    private void invalidLicenseFile(File licenseFile) {
        renameLicenseFile(licenseFile, ".invalid");
    }

    private void expiredLicenseFile(File licenseFile) {
        renameLicenseFile(licenseFile, ".expired");
    }

    private void renameLicenseFile(File licenseFile, String extension) {
        try {
            if (licenseFile.canWrite()) {
                Files.move(licenseFile, new File(licenseFile.getParent(), licenseFile.getName() + extension));
            }
        } catch (Exception e) {
            LOGGER.debug("Not able to rename expired license file {} to {}{}", licenseFile.getName(), extension);
        }
    }
}
