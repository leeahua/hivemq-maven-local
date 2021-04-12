package ae1;

import aa1.LicenseType;

public class LicenseInformationReader {

    public String read(LicenseInformation licenseInformation) {
        if (licenseInformation.getLicenseType() == LicenseType.NONE) {
            return noneInfo();
        }
        return classicInfo(licenseInformation);
    }

    private String classicInfo(LicenseInformation licenseInformation) {
        String ownerInfo = getOwnerInfo(licenseInformation);
        String siteInfo = getSiteInfo(licenseInformation.getLicense());
        String limitationInfo = getLimitationInfo(licenseInformation.getLicense());
        if (licenseInformation.getValidityEnd().getYear() >= 9999) {
            return "Found valid " + siteInfo +
                    " license (" + licenseInformation.getFileName() + ") issued to " +
                    ownerInfo + " " + limitationInfo + ".";
        }
        return "Found valid " + siteInfo +
                " license (" + licenseInformation.getFileName() + ") issued to " +
                ownerInfo + " " + limitationInfo + ", valid until " +
                licenseInformation.getValidityEnd().toString("yyyy-MM-dd") + ".";
    }

    private String getLimitationInfo(License license) {
        if (license.getMaximumConnections() == 0L) {
            return "without limitations";
        }
        return "for max " + license.getMaximumConnections() + " connections";
    }


    private String getSiteInfo(License license) {
        if (license.isSiteLicense()) {
            return "site";
        }
        return "single instance";
    }

    private String getOwnerInfo(LicenseInformation licenseInformation) {
        if (licenseInformation.getLicenseeCompany() != null &&
                licenseInformation.getLicenseeCompany().length() > 0) {
            return licenseInformation.getLicenseeCompany();
        }
        return licenseInformation.getLicenseeContact();
    }

    private String noneInfo() {
        return "No valid license file found. Using evaluation license, restricted to 25 connections.";
    }
}
