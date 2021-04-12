package ae1;

import ab1.LicenseFileVerifier;
import cb1.ManifestUtils;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.io.BaseEncoding;
import com.hivemq.HiveMQServer;
import com.hivemq.spi.annotations.NotNull;
import com.hivemq.spi.annotations.Nullable;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Singleton;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.util.Optional;

@Singleton
public class BuildTimeProducer {
    private static final Logger LOGGER = LoggerFactory.getLogger(BuildTimeProducer.class);
    private static final String SHA_256_WITH_RSA_ALGORITHM = "SHA256WithRSA";
    private static final String BOUNCYCASTLE_PROVIDER = "BC";
    public static final String BUILD_TIME_KEY = "Build-Time";
    public static final String BUILD_TIME_SIGNATURE_KEY = "Build-Time-Signature";
    private DateTime buildDate;

    @PostConstruct
    protected void init() {
        this.buildDate = readBuildTime();
        LOGGER.trace("Build Date: ", this.buildDate);
    }

    public DateTime getBuildDate() {
        return this.buildDate;
    }

    @VisibleForTesting
    @Nullable
    protected String getAttribute(String attribute) {
        return ManifestUtils.attribute(BuildTimeProducer.class, attribute);
    }

    private DateTime readBuildTime() {
        String buildTime = getAttribute(BUILD_TIME_KEY);
        String buildTimeSignature = getAttribute(BUILD_TIME_SIGNATURE_KEY);
        if (buildTime == null || buildTimeSignature == null) {
            LOGGER.warn("HiveMQ Distribution didn't contain a build date, please contact support@hivemq.com");
            return getDefaultBuildTime();
        }
        Optional<Certificate> mayCertificate = getCertificate();
        if (!mayCertificate.isPresent()) {
            return getDefaultBuildTime();
        }
        if (!verify(mayCertificate.get(),
                BaseEncoding.base64().decode(buildTimeSignature),
                buildTime.getBytes(StandardCharsets.UTF_8))) {
            LOGGER.error("Could not verify integrity of some HiveMQ files. Please contact support@hivemq.com");
            LOGGER.debug("The build signature does not match with the build time");
            return getDefaultBuildTime();
        }
        try {
            return ISODateTimeFormat.date().parseDateTime(buildTime);
        } catch (Exception e) {
            LOGGER.error("Could not read build time of this version. Please contact support@hivemq.com");
            LOGGER.debug("Original exception:", e);
        }
        return getDefaultBuildTime();
    }

    @NotNull
    @VisibleForTesting
    protected DateTime getDefaultBuildTime() {
        return DateTime.now();
    }


    private Optional<Certificate> getCertificate() {
        try {
            KeyStore keyStore = KeyStore.getInstance("JKS");
            ClassLoader classLoader = HiveMQServer.class.getClassLoader();
            URL url = classLoader.getResource("hivemq-build-cert.jks");
            InputStream inputStream = url.openStream();
            keyStore.load(inputStream, "7bmHxdzxotYMrxmea6tZEAWnceVZDaQBe8gZ7xECyURzRUdwZ".toCharArray());
            return Optional.ofNullable(keyStore.getCertificate(LicenseFileVerifier.CERTIFICATE_ALIAS));
        } catch (Exception e) {
            LOGGER.error("Could not verify integrity of some HiveMQ files. Please contact support@hivemq.com");
            LOGGER.debug("Could not read build time certificate. Original exception:", e);
        }
        return Optional.empty();
    }

    private boolean verify(Certificate certificate, byte[] sig, byte[] data) {
        try {
            Signature signature = Signature.getInstance(SHA_256_WITH_RSA_ALGORITHM, BOUNCYCASTLE_PROVIDER);
            signature.initVerify(certificate);
            signature.update(data);
            return signature.verify(sig);
        } catch (NoSuchAlgorithmException |
                NoSuchProviderException |
                InvalidKeyException |
                SignatureException e) {
            LOGGER.error("Could not verify integrity of some HiveMQ files. Please contact support@hivemq.com");
            LOGGER.debug("Original message:", e);
        }
        return false;
    }
}
