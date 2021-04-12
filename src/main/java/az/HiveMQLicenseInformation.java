package az;

import aa1.LicenseType;
import ae1.LicenseInformation;

import javax.inject.Inject;

public class HiveMQLicenseInformation extends AbstractInformation {
    private final LicenseInformation licenseInformation;

    @Inject
    protected HiveMQLicenseInformation(LicenseInformation licenseInformation) {
        this.licenseInformation = licenseInformation;
    }

    public String get() {
        StringBuilder builder = new StringBuilder();
        if (this.licenseInformation.getLicenseType() == LicenseType.NONE) {
            addInformation(builder, "License Type", "Free limited version");
        } else if (this.licenseInformation.getLicenseType() == LicenseType.CONNECTION_LIMITED) {
            addInformation(builder, "License Type", "Single Instance License");
            addLicenseeInformation(builder);
            addInformation(builder, "Creation Date", this.licenseInformation.getCreationDate().toString());
            addInformation(builder, "Validity End", this.licenseInformation.getValidityEnd().toString());
            addInformation(builder, "Validity Start", this.licenseInformation.getValidityStart().toString());
            addInformation(builder, "Supported Until", this.licenseInformation.getSupportedUntil().toString());
            addInformation(builder, "Site License", String.valueOf(this.licenseInformation.getLicense().isSiteLicense()));
            addInformation(builder, "Maximum Connections", String.valueOf(this.licenseInformation.getLicense().getMaximumConnections()));
        } else {
            return "No license information available\n";
        }
        return builder.toString();
    }

    private void addLicenseeInformation(StringBuilder builder) {
        addInformation(builder, "Licensee Company", this.licenseInformation.getLicenseeCompany());
        addInformation(builder, "Licensee Contact", this.licenseInformation.getLicenseeContact());
        addInformation(builder, "Licensee E-Mail", this.licenseInformation.getLicenseeEmail());
    }
}
