package aw;

import av.PersistenceConfig;
import com.google.common.base.Preconditions;
import com.hivemq.spi.annotations.NotNull;

public class PersistenceConfigImpl implements PersistenceConfig {
    private final boolean jmxEnabled;
    private final GCType gcType;
    private final int gcDeletionDelay;
    private final int gcRunPeriod;
    private final int gcFilesInterval;
    private final int gcMinAge;
    private final int syncPeriod;
    private final boolean durableWrites;

    public PersistenceConfigImpl(boolean jmxEnabled,
                                 @NotNull GCType gcType,
                                 int gcDeletionDelay,
                                 int gcRunPeriod,
                                 int gcFilesInterval,
                                 int gcMinAge,
                                 int syncPeriod,
                                 boolean durableWrites) {
        Preconditions.checkArgument(gcDeletionDelay >= 0, "GC deletion delay must be 0 or higher");
        Preconditions.checkNotNull(gcType, "GC Type must not be null");
        Preconditions.checkArgument(gcRunPeriod > 0, "GC run period must be higher than 0");
        Preconditions.checkArgument(gcFilesInterval > 0, "GC file interval must be higher than 0");
        Preconditions.checkArgument(gcMinAge > 0, "GC min age must be higher than 0");
        Preconditions.checkArgument(syncPeriod > 0, "Sync period must be higher than 0");
        this.jmxEnabled = jmxEnabled;
        this.gcType = gcType;
        this.gcDeletionDelay = gcDeletionDelay;
        this.gcRunPeriod = gcRunPeriod;
        this.gcFilesInterval = gcFilesInterval;
        this.gcMinAge = gcMinAge;
        this.syncPeriod = syncPeriod;
        this.durableWrites = durableWrites;
    }


    public boolean isJmxEnabled() {
        return jmxEnabled;
    }

    public GCType getGcType() {
        return gcType;
    }

    public int getGcDeletionDelay() {
        return gcDeletionDelay;
    }

    public int getGcRunPeriod() {
        return gcRunPeriod;
    }

    public int getGcFilesInterval() {
        return gcFilesInterval;
    }

    public int getGcMinAge() {
        return gcMinAge;
    }

    public int getSyncPeriod() {
        return syncPeriod;
    }

    public boolean isDurableWrites() {
        return durableWrites;
    }
}
