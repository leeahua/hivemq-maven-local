package av;

public interface PersistenceConfig {
    boolean isJmxEnabled();

    GCType getGcType();

    int getGcDeletionDelay();

    int getGcRunPeriod();

    int getGcFilesInterval();

    int getGcMinAge();

    int getSyncPeriod();

    boolean isDurableWrites();

    enum GCType {
        DELETE,
        RENAME
    }
}
