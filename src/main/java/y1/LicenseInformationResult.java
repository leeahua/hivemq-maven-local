package y1;


public class LicenseInformationResult {
    private final LicenseState licenseState;
    private final String licenseId;

    public LicenseInformationResult(LicenseState licenseState, String licenseId) {
        this.licenseState = licenseState;
        this.licenseId = licenseId;
    }

    public LicenseState getLicenseState() {
        return licenseState;
    }

    public String getLicenseId() {
        return licenseId;
    }
}
