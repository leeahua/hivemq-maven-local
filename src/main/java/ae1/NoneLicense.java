package ae1;

import java.io.Serializable;

public class NoneLicense implements License, Serializable {

    static NoneLicense newInstance() {
        return new NoneLicense();
    }

    public boolean isSiteLicense() {
        return false;
    }

    public boolean isInternetConnectionRequired() {
        return false;
    }

    public int getValidityGracePeriod() {
        return 0;
    }

    public long getMaximumConnections() {
        return LicenseInformation.DEFAULT_MAXIMUM_CONNECTIONS;
    }

    public long getConnectionsWarnThreshold() {
        return LicenseInformation.DEFAULT_CONNECTIONS_WARN_THRESHOLD;
    }
}
