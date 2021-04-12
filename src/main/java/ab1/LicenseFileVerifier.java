package ab1;

import aa1.LicenseType;
import com.hivemq.HiveMQServer;
import com.hivemq.spi.exceptions.UnrecoverableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;

public class LicenseFileVerifier implements SignatureVerifier {
    private static final Logger LOGGER = LoggerFactory.getLogger(LicenseFileVerifier.class);
    private static final String SHA_256_WITH_RSA_ALGORITHM = "SHA256WithRSA";
    private static final String BOUNCYCASTLE_PROVIDER = "BC";
    public static final String CERTIFICATE_ALIAS = "hivemq";
    private static final char[] KEYSTORE_PASSWORD = {'B', 'a', 'r', '8', 'D', 'F', 'o', 'X', 'Y', 'k', 'Z', 'y', 'H', 'k', 'r', 'q', '2', 'j', 'A', 'M', 'e', 'M', 'D', 'V', 'd', 'b', 'g', 'o', 'Z', 't'};

    public boolean verify(byte[] data, byte[] signature, LicenseType type) {
        if (LicenseType.CONNECTION_LIMITED.equals(type)) {
            return verify(getCertificate(), signature, data);
        }
        LOGGER.error("Unknown license type");
        return false;
    }

    protected boolean verify(Certificate certificate, byte[] sig, byte[] data) {
        try {
            Signature signature = Signature.getInstance(SHA_256_WITH_RSA_ALGORITHM, BOUNCYCASTLE_PROVIDER);
            signature.initVerify(certificate);
            signature.update(data);
            return signature.verify(sig);
        } catch (NoSuchAlgorithmException e) {
            LOGGER.error("There is no SHA 256 with RSA Algorithm. Can not verify the license file!", e);
        } catch (NoSuchProviderException e) {
            LOGGER.error("Bouncycastle Provider not found. Can not verify the license file!", e);
        } catch (InvalidKeyException e) {
            LOGGER.error("Key/Certificate is invalid or corrupt. Can not verify the license file!", e);
        } catch (SignatureException e) {
            LOGGER.error("Error with the signature. Can not verify the license file!", e);
        }
        return false;
    }

    public Certificate getCertificate() {
        try {
            KeyStore keyStore = KeyStore.getInstance("JKS");
            ClassLoader classLoader = HiveMQServer.class.getClassLoader();
            URL url = classLoader.getResource("hivemq.jks");
            InputStream inputStream = url.openStream();
            keyStore.load(inputStream, KEYSTORE_PASSWORD);
            return keyStore.getCertificate(CERTIFICATE_ALIAS);
        } catch (CertificateException | NoSuchAlgorithmException | KeyStoreException e) {
            LOGGER.error("A fatal error occurred while loading the keystore.", e);
        } catch (FileNotFoundException localFileNotFoundException) {
            LOGGER.error("Fatal: Internal Keystore not found.");
        } catch (Exception localException) {
            LOGGER.error("A fatal error occurred while loading the keystore.", localException);
        }
        throw new UnrecoverableException();
    }
}
