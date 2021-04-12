package af1;

import ab1.Decrypter;
import ab1.LicenseFileDecrypter;
import ab1.LicenseFileVerifier;
import ab1.SignatureVerifier;
import ae1.LicenseInformation;
import ag1.Licensing;
import ah1.LicenseConnectionLimiter;
import ai1.LicenseFolderMonitor;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.hivemq.spi.exceptions.UnrecoverableException;
import d.CacheScoped;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKeyFactory;
import javax.inject.Singleton;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

public class LicensingModule extends AbstractModule {
    private static final Logger LOGGER = LoggerFactory.getLogger(LicensingModule.class);
    public static final String SECRET_KEY_ALGORITHM = "PBKDF2WithHmacSHA1";
    public static final String CIPHER_ALGORITHM = "AES/CBC/PKCS5Padding";

    protected void configure() {
        addSecurityProvider();
        install(new LicensingSchedulerModule());
        bind(SignatureVerifier.class).to(LicenseFileVerifier.class);
        bind(LicenseConnectionLimiter.class).in(Singleton.class);
        bind(LicenseFolderMonitor.class).asEagerSingleton();
        bind(Decrypter.class).to(LicenseFileDecrypter.class);
        bind(ScheduledExecutorService.class).annotatedWith(Licensing.class).toInstance(new ScheduledThreadPoolExecutor(1));
        bind(LicenseInformation.class).toProvider(LicenseInformationService.class);
    }

    protected void addSecurityProvider() {
        Security.addProvider(new BouncyCastleProvider());
    }

    @Provides
    @CacheScoped
    public SecretKeyFactory provideSecretKeyFactory() {
        try {
            return SecretKeyFactory.getInstance(SECRET_KEY_ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            LOGGER.error("Fatal: Could not find Algorithm {}. Exiting HiveMQ.", SECRET_KEY_ALGORITHM, e);
            throw new UnrecoverableException();
        }
    }

    @Provides
    @CacheScoped
    public Cipher provideCipher() {
        try {
            return Cipher.getInstance(CIPHER_ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            LOGGER.error("Fatal: Could not find Algorithm {}. Exiting HiveMQ.", CIPHER_ALGORITHM, e);
            throw new UnrecoverableException();
        } catch (NoSuchPaddingException e) {
            LOGGER.error("Fatal: Could not find padding for Algorithm {}. Exiting HiveMQ.", CIPHER_ALGORITHM, e);
            throw new UnrecoverableException();
        }
    }
}
