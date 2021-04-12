package w1;

import com.hivemq.spi.message.PubRel;
import j1.ClusterRequest;

public class ClusterPubRelRequest implements ClusterRequest {
    private final PubRel pubRel;
    private final String clientId;

    public ClusterPubRelRequest(PubRel pubRel, String clientId) {
        this.pubRel = pubRel;
        this.clientId = clientId;
    }

    public PubRel getPubRel() {
        return pubRel;
    }

    public String getClientId() {
        return clientId;
    }
}
