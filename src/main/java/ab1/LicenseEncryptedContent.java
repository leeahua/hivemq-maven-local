package ab1;

import aa1.LicenseType;

public class LicenseEncryptedContent {
    private LicenseType type;
    private byte[] initializationVector;
    private byte[] salt;
    private byte[] cypherText;

    public LicenseType getType() {
        return type;
    }

    public void setType(LicenseType type) {
        this.type = type;
    }

    public byte[] getInitializationVector() {
        return initializationVector;
    }

    public void setInitializationVector(byte[] initializationVector) {
        this.initializationVector = initializationVector;
    }

    public byte[] getSalt() {
        return salt;
    }

    public void setSalt(byte[] salt) {
        this.salt = salt;
    }

    public byte[] getCypherText() {
        return cypherText;
    }

    public void setCypherText(byte[] cypherText) {
        this.cypherText = cypherText;
    }
}
