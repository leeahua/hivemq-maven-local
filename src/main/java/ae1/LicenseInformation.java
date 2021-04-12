package ae1;

import aa1.LicenseType;
import com.google.common.base.Preconditions;
import com.hivemq.spi.annotations.NotNull;
import org.joda.time.DateTime;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class LicenseInformation implements Serializable {
    public static final long DEFAULT_MAXIMUM_CONNECTIONS = 25L;
    public static final long DEFAULT_CONNECTIONS_WARN_THRESHOLD = 23L;
    private static final long serialVersionUID = -6199529416905073822L;
    private final String id;
    private final String fileName;
    private final DateTime creationDate;
    private final String licenseeCompany;
    private final String licenseeContact;
    private final String licenseeEmail;
    private final String vendorCompany;
    private final String vendorContact;
    private final String vendorEmail;
    private final String vendorWebsite;
    private final DateTime validityStart;
    private final DateTime validityEnd;
    private final DateTime supportedUntil;
    private final LicenseType licenseType;
    private final License license;

    private LicenseInformation(String id,
                               DateTime creationDate,
                               String fileName,
                               String licenseeCompany,
                               String licenseeContact,
                               String licenseeEmail,
                               String vendorCompany,
                               String vendorContact,
                               String vendorEmail,
                               String vendorWebsite,
                               DateTime validityStart,
                               DateTime validityEnd,
                               DateTime supportedUntil,
                               LicenseType licenseType,
                               License license) {
        this.id = id;
        this.creationDate = creationDate;
        this.fileName = fileName;
        this.licenseeCompany = licenseeCompany;
        this.licenseeContact = licenseeContact;
        this.licenseeEmail = licenseeEmail;
        this.vendorCompany = vendorCompany;
        this.vendorContact = vendorContact;
        this.vendorEmail = vendorEmail;
        this.vendorWebsite = vendorWebsite;
        this.validityStart = validityStart;
        this.validityEnd = validityEnd;
        this.supportedUntil = supportedUntil;
        this.licenseType = licenseType;
        this.license = license;
    }

    @NotNull
    public String getId() {
        return id;
    }

    public String getFileName() {
        return fileName;
    }

    public DateTime getCreationDate() {
        return creationDate;
    }

    public String getLicenseeCompany() {
        return licenseeCompany;
    }

    public String getLicenseeContact() {
        return licenseeContact;
    }

    public String getLicenseeEmail() {
        return licenseeEmail;
    }

    public String getVendorCompany() {
        return vendorCompany;
    }

    public String getVendorContact() {
        return vendorContact;
    }

    public String getVendorEmail() {
        return vendorEmail;
    }

    public String getVendorWebsite() {
        return vendorWebsite;
    }

    public DateTime getValidityStart() {
        return validityStart;
    }

    public DateTime getValidityEnd() {
        return validityEnd;
    }

    public DateTime getSupportedUntil() {
        return supportedUntil;
    }

    @NotNull
    public LicenseType getLicenseType() {
        return licenseType;
    }

    public License getLicense() {
        return license;
    }

    public static LicenseInformation createDefault() {
        return new Builder()
                .withNoneLicense()
                .withId("NONE-" + UUID.randomUUID())
                .withFileName("")
                .withVendorCompany("dc-square GmbH")
                .withVendorWebsite("http://www.hivemq.com")
                .withVendorContact("dc-square GmbH")
                .withVendorEmail("support@hivemq.com")
                .withLicenseeCompany("unknown")
                .withLicenseeContact("unknown")
                .withLicenseeEmail("support@hivemq.com")
                .build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        LicenseInformation that = (LicenseInformation) o;
        return Objects.equals(fileName, that.fileName) &&
                Objects.equals(id, that.id) &&
                Objects.equals(creationDate, that.creationDate) &&
                Objects.equals(licenseeCompany, that.licenseeCompany) &&
                Objects.equals(licenseeContact, that.licenseeContact) &&
                Objects.equals(licenseeEmail, that.licenseeEmail) &&
                Objects.equals(vendorCompany, that.vendorCompany) &&
                Objects.equals(vendorContact, that.vendorContact) &&
                Objects.equals(vendorEmail, that.vendorEmail) &&
                Objects.equals(vendorWebsite, that.vendorWebsite) &&
                Objects.equals(validityStart, that.validityStart) &&
                Objects.equals(validityEnd, that.validityEnd) &&
                Objects.equals(supportedUntil, that.supportedUntil) &&
                licenseType == that.licenseType &&
                Objects.equals(license, that.license);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fileName, id, creationDate,
                licenseeCompany, licenseeContact, licenseeEmail,
                vendorCompany, vendorContact, vendorEmail, vendorWebsite,
                validityStart, validityEnd, supportedUntil,
                licenseType, license);
    }

    public static final class Builder implements Serializable {
        private String id;
        private DateTime creationDate;
        private String fileName;
        private String licenseeCompany;
        private String licenseeContact;
        private String licenseeEmail;
        private String vendorCompany;
        private String vendorContact;
        private String vendorEmail;
        private String vendorWebsite;
        private DateTime validityStart;
        private DateTime validityEnd;
        private DateTime supportedUntil;
        private LicenseType licenseType;
        private License license;

        public Builder withId(String id) {
            this.id = id;
            return this;
        }

        public Builder withCreationDate(DateTime creationDate) {
            this.creationDate = creationDate;
            return this;
        }

        public Builder withFileName(String fileName) {
            this.fileName = fileName;
            return this;
        }

        public Builder withLicenseeCompany(String licenseeCompany) {
            this.licenseeCompany = licenseeCompany;
            return this;
        }

        public Builder withLicenseeContact(String licenseeContact) {
            this.licenseeContact = licenseeContact;
            return this;
        }

        public Builder withLicenseeEmail(String licenseeEmail) {
            this.licenseeEmail = licenseeEmail;
            return this;
        }

        public Builder withVendorCompany(String vendorCompany) {
            this.vendorCompany = vendorCompany;
            return this;
        }

        public Builder withVendorContact(String vendorContact) {
            this.vendorContact = vendorContact;
            return this;
        }

        public Builder withVendorEmail(String vendorEmail) {
            this.vendorEmail = vendorEmail;
            return this;
        }

        public Builder withVendorWebsite(String vendorWebsite) {
            this.vendorWebsite = vendorWebsite;
            return this;
        }

        public Builder withValidityStart(DateTime validityStart) {
            this.validityStart = validityStart;
            return this;
        }

        public Builder withValidityEnd(DateTime validityEnd) {
            this.validityEnd = validityEnd;
            return this;
        }

        public Builder withSupportedUntil(DateTime supportedUntil) {
            this.supportedUntil = supportedUntil;
            return this;
        }

        public Builder withClassicLicense(ClassicLicense siteLicense) {
            this.licenseType = LicenseType.CONNECTION_LIMITED;
            this.license = siteLicense;
            return this;
        }

        public Builder withNoneLicense() {
            this.licenseType = LicenseType.NONE;
            this.license = NoneLicense.newInstance();
            return this;
        }

        public LicenseInformation build() {
            Preconditions.checkNotNull(this.id, "A LicenseInformation must always contain an ID !");
            Preconditions.checkNotNull(this.licenseType, "A LicenseInformation must always contain a type !");
            Preconditions.checkNotNull(this.license, "A single instance license information must be present");
            return new LicenseInformation(this.id, this.creationDate, this.fileName,
                    this.licenseeCompany, this.licenseeContact, this.licenseeEmail,
                    this.vendorCompany, this.vendorContact, this.vendorEmail, this.vendorWebsite,
                    this.validityStart, this.validityEnd, this.supportedUntil,
                    this.licenseType, this.license);
        }
    }
}
