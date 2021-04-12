package at1;

public enum MigrationType {
    FILE_PERSISTENCE_CLIENT_SESSIONS("client session file persistence"),
    FILE_PERSISTENCE_CLIENT_SESSION_QUEUED_MESSAGES("queued messages file persistence"),
    FILE_PERSISTENCE_CLIENT_SESSION_SUBSCRIPTIONS("client subscriptions file persistence"),
    FILE_PERSISTENCE_INCOMING_MESSAGE_FLOW("incoming message flow file persistence"),
    FILE_PERSISTENCE_OUTGOING_MESSAGE_FLOW("outgoing message flow file persistence"),
    FILE_PERSISTENCE_RETAINED_MESSAGES("retained message flow file persistence");

    private final String type;

    MigrationType(String type) {
        this.type = type;
    }

    public String toString() {
        return this.type;
    }
}
