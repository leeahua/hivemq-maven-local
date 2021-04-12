package ab1;

import ac1.InvalidLicenseException;

public interface Decrypter {
    String decrypt(byte[] iv, byte[] salt, byte[] cipherText, char[] password) throws InvalidLicenseException;
}
