package ad1;

import aa1.LicenseType;
import ab1.LicenseContent;
import ac1.LicenseFileCorruptException;
import ae1.ClassicLicense;
import ae1.LicenseInformation;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;
import java.util.Properties;

public class LicenseInformationFactory {
    private static final DateTimeFormatter FORMATTER = ISODateTimeFormat.dateTime().withZoneUTC();

    public LicenseInformation create(LicenseContent content, String fileName) throws LicenseFileCorruptException {
        try {
            Properties properties = content.getProperties();
            LicenseInformation.Builder builder = new LicenseInformation.Builder();
            builder.withFileName(fileName);
            builder.withId(properties.getProperty(LicenseInformations.ID));
            builder.withCreationDate(FORMATTER.parseDateTime(properties.getProperty(LicenseInformations.CREATION_DATE).trim()));
            builder.withLicenseeCompany(properties.getProperty(LicenseInformations.LICENSEE_COMPANY));
            builder.withLicenseeContact(properties.getProperty(LicenseInformations.LICENSEE_CONTACT));
            builder.withLicenseeEmail(properties.getProperty(LicenseInformations.LICENSEE_EMAIL));
            builder.withVendorCompany(properties.getProperty(LicenseInformations.VENDOR_COMPANY));
            builder.withVendorContact(properties.getProperty(LicenseInformations.VENDOR_CONTACT));
            builder.withVendorEmail(properties.getProperty(LicenseInformations.VENDOR_EMAIL));
            builder.withVendorWebsite(properties.getProperty(LicenseInformations.VENDOR_WEBSITE));
            builder.withValidityStart(FORMATTER.parseDateTime(properties.getProperty(LicenseInformations.VALIDITY_START).trim()));
            builder.withValidityEnd(FORMATTER.parseDateTime(properties.getProperty(LicenseInformations.VALIDITY_END).trim()));
            if (properties.getProperty(LicenseInformations.VALIDITY_SUPPORTED_UNTIL) != null) {
                builder.withSupportedUntil(FORMATTER.parseDateTime(properties.getProperty(LicenseInformations.VALIDITY_SUPPORTED_UNTIL).trim()));
            }
            if (!LicenseType.CONNECTION_LIMITED.equals(content.getType())) {
                throw new LicenseFileCorruptException("Unknown license type");
            }
            ClassicLicense.Builder classicLicenseBuilder = new ClassicLicense.Builder();
            classicLicenseBuilder.withSiteLicense(Boolean.parseBoolean(properties.getProperty(LicenseInformations.SITE_LICENSE)));
            classicLicenseBuilder.withInternetConnectionRequired(Boolean.parseBoolean(properties.getProperty(LicenseInformations.INTERNET_CONNECTION_REQUIRED).trim()));
            classicLicenseBuilder.withValidityGracePeriod(Integer.parseInt(properties.getProperty(LicenseInformations.VALIDITY_GRACE_PERIOD).trim()));
            classicLicenseBuilder.withMaximumConnections(Long.parseLong(properties.getProperty(LicenseInformations.LIMITATIONS_CONNECTIONS_TOTAL).trim()));
            classicLicenseBuilder.withConnectionsWarnThreshold(Long.parseLong(properties.getProperty(LicenseInformations.LIMITATIONS_CONNECTIONS_WARNTHRESHOLD).trim()));
            builder.withClassicLicense(classicLicenseBuilder.build());
            return builder.build();
        } catch (NoSuchElementException e) {
            throw new LicenseFileCorruptException("License Information does not contain all required values", e);
        }
    }
}
