package ae1;

public interface License {
    boolean isSiteLicense();

    boolean isInternetConnectionRequired();

    int getValidityGracePeriod();

    long getMaximumConnections();

    long getConnectionsWarnThreshold();
}
