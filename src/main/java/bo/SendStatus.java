package bo;

public enum SendStatus {
    DELIVERED,
    NOT_CONNECTED,
    MESSAGE_IDS_EXHAUSTED,
    QUEUED,
    FAILED,
    IN_PROGRESS,
    PERSISTENT_SESSION;


    private SendStatus() {
    }
}
