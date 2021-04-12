package ab1;

import aa1.LicenseType;

public interface SignatureVerifier {
    boolean verify(byte[] data, byte[] signature, LicenseType type);
}
