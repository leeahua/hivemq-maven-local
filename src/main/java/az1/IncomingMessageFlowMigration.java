package az1;

import at1.Migration;

public class IncomingMessageFlowMigration implements Migration {
    public void migrate(String toVersion) {
        throw new IllegalArgumentException("Unknown version " + toVersion + " for incoming message flow migration");
    }
}
