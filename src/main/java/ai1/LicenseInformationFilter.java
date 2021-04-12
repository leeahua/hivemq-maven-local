package ai1;

import aa1.LicenseType;
import ae1.LicenseInformation;
import org.joda.time.DateTime;

import javax.inject.Singleton;
import java.util.Collection;
import java.util.Iterator;

@Singleton
public class LicenseInformationFilter {
    public LicenseInformation optimal(Collection<LicenseInformation> licenseInformations,
                                      LicenseType type) {
        if (licenseInformations.size() < 1) {
            return LicenseInformation.createDefault();
        }
        LicenseInformation optimalLicense = optimalLicenseInformation(licenseInformations, type);
        if (optimalLicense != null) {
            return optimalLicense;
        }
        return LicenseInformation.createDefault();
    }

    private LicenseInformation optimalLicenseInformation(Collection<LicenseInformation> licenseInformations,
                                                         LicenseType type) {
        LicenseInformation licenseInformation = null;
        Iterator<LicenseInformation> iterator = licenseInformations.iterator();
        while (iterator.hasNext()) {
            LicenseInformation information = iterator.next();
            if (sameType(type, information) &&
                    !expired(information) &&
                    !information.getValidityStart()
                            .withHourOfDay(0).withSecondOfMinute(0).withSecondOfMinute(0).isAfter(DateTime.now())) {
                if (licenseInformation == null) {
                    licenseInformation = information;
                } else if (isGreaterThanMaximumConnections(information, licenseInformation)) {
                    licenseInformation = information;
                } else if (isAfterValidityEnd(information, licenseInformation)) {
                    licenseInformation = information;
                }
            }
        }
        return licenseInformation;
    }

    protected boolean isAfterValidityEnd(LicenseInformation current, LicenseInformation original) {
        if (current.getLicense().getMaximumConnections() !=
                original.getLicense().getMaximumConnections()) {
            return false;
        }
        return current.getValidityEnd().isAfter(original.getValidityEnd()
                .withHourOfDay(23).withMinuteOfHour(59).withSecondOfMinute(59));
    }

    protected boolean isGreaterThanMaximumConnections(LicenseInformation current, LicenseInformation original) {
        if (original.getLicense().getMaximumConnections() == 0L) {
            return false;
        }
        if (current.getLicense().getMaximumConnections() == 0L) {
            return true;
        }
        return current.getLicense().getMaximumConnections() >
                original.getLicense().getMaximumConnections();
    }

    protected boolean expired(LicenseInformation licenseInformation) {
        return licenseInformation.getValidityEnd()
                .isBefore(DateTime.now()
                        .withMinuteOfHour(0)
                        .withHourOfDay(0)
                        .withSecondOfMinute(0));
    }


    protected boolean sameType(LicenseType type, LicenseInformation licenseInformation) {
        if (LicenseType.NONE.equals(type)) {
            return true;
        }
        if (LicenseType.NONE.equals(licenseInformation.getLicenseType())) {
            return true;
        }
        return licenseInformation.getLicenseType().equals(type);
    }
}
