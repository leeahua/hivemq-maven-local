package ae1;

import com.google.common.base.Preconditions;

import java.io.Serializable;
import java.util.Objects;

public class ClassicLicense implements License, Serializable {
    private final boolean siteLicense;
    private final boolean internetConnectionRequired;
    private final int validityGracePeriod;
    private final long maximumConnections;
    private final long connectionsWarnThreshold;

    private ClassicLicense(boolean siteLicense,
                           boolean internetConnectionRequired,
                           int validityGracePeriod,
                           long maximumConnections,
                           long connectionsWarnThreshold) {
        this.siteLicense = siteLicense;
        this.internetConnectionRequired = internetConnectionRequired;
        this.validityGracePeriod = validityGracePeriod;
        this.maximumConnections = maximumConnections;
        this.connectionsWarnThreshold = connectionsWarnThreshold;
    }

    public boolean isSiteLicense() {
        return siteLicense;
    }

    @Override
    public boolean isInternetConnectionRequired() {
        return internetConnectionRequired;
    }

    @Override
    public int getValidityGracePeriod() {
        return validityGracePeriod;
    }

    @Override
    public long getMaximumConnections() {
        return maximumConnections;
    }

    @Override
    public long getConnectionsWarnThreshold() {
        return connectionsWarnThreshold;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ClassicLicense that = (ClassicLicense) o;
        return siteLicense == that.siteLicense &&
                internetConnectionRequired == that.internetConnectionRequired &&
                validityGracePeriod == that.validityGracePeriod &&
                maximumConnections == that.maximumConnections &&
                connectionsWarnThreshold == that.connectionsWarnThreshold;
    }

    @Override
    public int hashCode() {
        return Objects.hash(siteLicense, internetConnectionRequired,
                validityGracePeriod, maximumConnections, connectionsWarnThreshold);
    }

    public static class Builder {
        private Boolean siteLicense = false;
        private Boolean internetConnectionRequired = false;
        private Integer validityGracePeriod = 0;
        private Long maximumConnections = null;
        private Long connectionsWarnThreshold = null;

        public Builder withSiteLicense(boolean siteLicense) {
            this.siteLicense = siteLicense;
            return this;
        }

        public Builder withInternetConnectionRequired(boolean internetConnectionRequired) {
            this.internetConnectionRequired = internetConnectionRequired;
            return this;
        }

        public Builder withValidityGracePeriod(int validityGracePeriod) {
            this.validityGracePeriod = validityGracePeriod;
            return this;
        }

        public Builder withMaximumConnections(long maximumConnections) {
            this.maximumConnections = maximumConnections;
            return this;
        }

        public Builder withConnectionsWarnThreshold(long connectionsWarnThreshold) {
            this.connectionsWarnThreshold = connectionsWarnThreshold;
            return this;
        }

        public ClassicLicense build() {
            Preconditions.checkNotNull(this.maximumConnections, "Max total connections missing");
            Preconditions.checkNotNull(this.connectionsWarnThreshold, "connections warn threshold missing");
            return new ClassicLicense(this.siteLicense,
                    this.internetConnectionRequired, this.validityGracePeriod,
                    this.maximumConnections, this.connectionsWarnThreshold);
        }
    }
}
