package at1;

import av1.HiveMQVersionComparator;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.hivemq.spi.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

public class MigrationVersions {
    private static final Map<String, Set<MigrationType>> versionMigrationTypes = new HashMap();

    public static Map<MigrationType, Set<String>> getNeededMigrations(String previousVersion, String currentVersion) {
        Preconditions.checkNotNull(previousVersion, "Previous version cannot be null");
        Preconditions.checkNotNull(currentVersion, "Current version cannot be null");
        if (previousVersion.equals(currentVersion)) {
            return Maps.newHashMap();
        }
        SortedSet<String> tallVersions = getTallVersions(previousVersion);
        Map<MigrationType, Set<String>> neededMigrations = new HashMap<>();
        tallVersions.forEach(tallVersion -> {
            Set<MigrationType> types = versionMigrationTypes.get(tallVersion);
            types.forEach(type->{
                Set<String> versions = neededMigrations.get(type);
                if(versions == null){
                    versions = Sets.newTreeSet(new HiveMQVersionComparator());
                }
                neededMigrations.put(type, versions);
            });
        });
        return neededMigrations;
    }

    @NotNull
    private static SortedSet<String> getTallVersions(String previousVersion) {
        HiveMQVersionComparator comparator = new HiveMQVersionComparator();
        TreeSet<String> versions = Sets.newTreeSet(comparator);
        versions.addAll(versionMigrationTypes.keySet());
        versions.stream()
                .filter(version -> comparator.compare(version, previousVersion) <= 0)
                .forEach(versions::remove);
        return versions;
    }

    static {
        versionMigrationTypes.put("3.1.0", Sets.newHashSet(
                MigrationType.FILE_PERSISTENCE_CLIENT_SESSIONS,
                MigrationType.FILE_PERSISTENCE_CLIENT_SESSION_QUEUED_MESSAGES,
                MigrationType.FILE_PERSISTENCE_CLIENT_SESSION_SUBSCRIPTIONS,
                MigrationType.FILE_PERSISTENCE_OUTGOING_MESSAGE_FLOW,
                MigrationType.FILE_PERSISTENCE_RETAINED_MESSAGES));
    }
}
