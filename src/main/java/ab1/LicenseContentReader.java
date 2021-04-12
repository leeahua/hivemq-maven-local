package ab1;

import aa1.LicenseType;
import ac1.InvalidLicenseException;
import ac1.LicenseFileCorruptException;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Charsets;
import com.google.common.io.BaseEncoding;
import com.google.common.io.Files;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.Properties;

public class LicenseContentReader {
    private static final byte[] PASSWORD = {-38, -3, -105, -65, -86, -72, Byte.MIN_VALUE, 61, 88, -38, -105, -115, 1, -105, 125, -116, 10, 94, -44, 93, 100, -86, 31, -124, 91, -79, -53, 55, -53, -85, -121, 40, -25, 29, 97, 99, 15, -9, 9, 4, 52, 35, 116, 64, 24, -43, -84, 55, -103, -45};
    public static final String SEPARATOR = "------";
    public static final String HMQ_3 = "H!MQ$[3]";
    public static final String PAYG = "PAYG";
    private static final Logger LOGGER = LoggerFactory.getLogger(LicenseContentReader.class);
    private final Decrypter decrypter;
    private final SignatureVerifier signatureVerifier;
    static final String HMQ_2_X_NOT_SUPPORTED = "HiveMQ 2.x licenses are not supported with HiveMQ 3.x. Please contact sales@hivemq.com";
    static final String LICENSE_FILE_IS_CORRUPT = "The license file is corrupt. Please contact sales@hivemq.com";
    static final String PAYG_NOT_SUPPORTED = "pay-as-you-go licenses are not supported with HiveMQ 3.x. Please contact sales@hivemq.com";

    @Inject
    LicenseContentReader(Decrypter decrypter, SignatureVerifier signatureVerifier) {
        this.decrypter = decrypter;
        this.signatureVerifier = signatureVerifier;
    }

    public LicenseContent read(File licenseFile) throws FileNotFoundException, LicenseFileCorruptException {
        if (!licenseFile.exists()) {
            throw new FileNotFoundException("Licensing File does not exist");
        }
        if (!licenseFile.canRead() || !licenseFile.isFile()) {
            throw new LicenseFileCorruptException("License file " + licenseFile.getAbsolutePath() + " not usable.");
        }
        LicenseEncryptedContent encryptedContent = readEncryptedContent(licenseFile);
        String decryptedContent = decrypt(encryptedContent);
        String licenseContent = getLicenseContent(decryptedContent);
        String signature = getSignature(decryptedContent);
        boolean verified = verifyAuthenticity(licenseContent, signature, encryptedContent.getType());
        if (verified) {
            return new LicenseContent(encryptedContent.getType(), getProperties(licenseContent));
        }
        throw new LicenseFileCorruptException("Verification of the signature and license file failed!");
    }

    @VisibleForTesting
    protected String getLicenseContent(String decryptedContent) {
        return StringUtils.substringBefore(decryptedContent, SEPARATOR);
    }

    @VisibleForTesting
    protected String getSignature(String decryptedContent) {
        return StringUtils.substringAfter(decryptedContent, SEPARATOR);
    }

    private Properties getProperties(String licenseContent) {
        Properties properties = new Properties();
        try {
            properties.load(new StringReader(licenseContent));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return properties;
    }

    @VisibleForTesting
    LicenseEncryptedContent readEncryptedContent(File licenseFile) throws LicenseFileCorruptException, FileNotFoundException {
        BaseEncoding base64 = BaseEncoding.base64();
        try {
            List<String> lines = Files.readLines(licenseFile, Charsets.UTF_8);
            if (lines.size() == 3) {
                throw new LicenseFileCorruptException(HMQ_2_X_NOT_SUPPORTED);
            }
            if (lines.size() != 4) {
                throw new LicenseFileCorruptException(LICENSE_FILE_IS_CORRUPT);
            }
            String licenseTypeLine = lines.get(0);
            if (!HMQ_3.equals(licenseTypeLine)) {
                if (PAYG.equals(licenseTypeLine)) {
                    throw new LicenseFileCorruptException(PAYG_NOT_SUPPORTED);
                }
                throw new LicenseFileCorruptException(LICENSE_FILE_IS_CORRUPT);
            }
            LicenseType licenseType = LicenseType.CONNECTION_LIMITED;
            int startLineIndex = 1;
            LicenseEncryptedContent encryptedContent = new LicenseEncryptedContent();
            encryptedContent.setType(licenseType);
            encryptedContent.setInitializationVector(base64.decode(lines.get(startLineIndex)));
            encryptedContent.setSalt(base64.decode(lines.get(1 + startLineIndex)));
            encryptedContent.setCypherText(base64.decode(lines.get(2 + startLineIndex)));
            return encryptedContent;
        } catch (FileNotFoundException e) {
            throw e;
        } catch (IOException e) {
            throw new LicenseFileCorruptException("Error while reading the license file", e);
        }
    }

    private String decrypt(LicenseEncryptedContent encryptedContent) throws LicenseFileCorruptException {
        try {
            if (encryptedContent.getType().equals(LicenseType.CONNECTION_LIMITED)) {
                return this.decrypter.decrypt(encryptedContent.getInitializationVector(),
                        encryptedContent.getSalt(),
                        encryptedContent.getCypherText(),
                        PasswordDecrypter.decrypt(PASSWORD));
            }
            throw new LicenseFileCorruptException("Unknown license type");
        } catch (InvalidLicenseException e) {
            throw new LicenseFileCorruptException("License file is corrupt!", e);
        }
    }

    private boolean verifyAuthenticity(String licenseContent, String sig, LicenseType type) {
        byte[] signature;
        try {
            signature = BaseEncoding.base64().decode(sig);
        } catch (IllegalArgumentException e) {
            this.LOGGER.debug("License signature is not a valid Base64 encoding.", e);
            return false;
        }
        byte[] data = licenseContent.getBytes(Charsets.UTF_8);
        return this.signatureVerifier.verify(data, signature, type);
    }
}
