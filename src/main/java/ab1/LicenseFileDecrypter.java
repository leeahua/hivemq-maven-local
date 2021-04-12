package ab1;

import ac1.InvalidLicenseException;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Charsets;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import javax.inject.Inject;
import javax.inject.Provider;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.spec.InvalidKeySpecException;

public class LicenseFileDecrypter implements Decrypter {
    private static final int ITERATIONS = 65536;
    private static final int KEY_LENGTH = 128;
    private final Provider<SecretKeyFactory> factoryProvider;
    private final Provider<Cipher> cipherProvider;

    @VisibleForTesting
    @Inject
    public LicenseFileDecrypter(Provider<SecretKeyFactory> factoryProvider,
                                Provider<Cipher> cipherProvider) {
        this.factoryProvider = factoryProvider;
        this.cipherProvider = cipherProvider;
    }

    public String decrypt(byte[] iv, byte[] salt, byte[] cipherText, char[] password) throws InvalidLicenseException {
        try {
            PBEKeySpec keySpec = new PBEKeySpec(password, salt, ITERATIONS, KEY_LENGTH);
            SecretKey secretKey = this.factoryProvider.get().generateSecret(keySpec);
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getEncoded(), "AES");
            this.cipherProvider.get().init(Cipher.DECRYPT_MODE, secretKeySpec, new IvParameterSpec(iv));
            return new String(this.cipherProvider.get().doFinal(cipherText), Charsets.UTF_8);
        } catch (BadPaddingException |
                InvalidAlgorithmParameterException |
                IllegalBlockSizeException |
                InvalidKeyException |
                InvalidKeySpecException e) {
            throw new InvalidLicenseException(e);
        }
    }
}
