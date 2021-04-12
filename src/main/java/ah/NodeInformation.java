package ah;

public class NodeInformation {
    private String version;
    private Long lastRemovedMillis;
    private Long lastAddedMillis;

    public NodeInformation() {
    }

    public NodeInformation(String version, Long lastRemovedMillis, Long lastAddedMillis) {
        this.version = version;
        this.lastRemovedMillis = lastRemovedMillis;
        this.lastAddedMillis = lastAddedMillis;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Long getLastRemovedMillis() {
        return lastRemovedMillis;
    }

    public void setLastRemovedMillis(Long lastRemovedMillis) {
        this.lastRemovedMillis = lastRemovedMillis;
    }

    public Long getLastAddedMillis() {
        return lastAddedMillis;
    }

    public void setLastAddedMillis(Long lastAddedMillis) {
        this.lastAddedMillis = lastAddedMillis;
    }
}
