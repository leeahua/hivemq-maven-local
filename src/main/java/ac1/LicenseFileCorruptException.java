package ac1;

public class LicenseFileCorruptException extends Exception {
    public LicenseFileCorruptException() {
    }

    public LicenseFileCorruptException(String message) {
        super(message);
    }

    public LicenseFileCorruptException(String message, Throwable cause) {
        super(message, cause);
    }

    public LicenseFileCorruptException(Throwable cause) {
        super(cause);
    }
}
