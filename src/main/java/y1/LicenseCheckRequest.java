package y1;

import j1.ClusterRequest;

public class LicenseCheckRequest implements ClusterRequest {
    private final LicenseState licenseState;
    private final String licenseId;

    public LicenseCheckRequest(LicenseState licenseState, String licenseId) {
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
