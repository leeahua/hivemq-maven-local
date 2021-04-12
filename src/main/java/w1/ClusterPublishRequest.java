package w1;

import bu.InternalPublish;
import j1.ClusterRequest;
// TODO:
public class ClusterPublishRequest implements ClusterRequest {
    private final InternalPublish publish;
    private final int qoSNumber;
    private final String clientId;
    private final boolean d;
    private final boolean e;
    private final boolean f;

    public ClusterPublishRequest(InternalPublish publish,
                                 int qoSNumber,
                                 String clientId,
                                 boolean paramBoolean1,
                                 boolean paramBoolean2,
                                 boolean paramBoolean3) {
        this.publish = publish;
        this.qoSNumber = qoSNumber;
        this.clientId = clientId;
        this.d = paramBoolean1;
        this.e = paramBoolean2;
        this.f = paramBoolean3;
    }

    public InternalPublish getPublish() {
        return this.publish;
    }

    public int getQoSNumber() {
        return qoSNumber;
    }

    public String getClientId() {
        return this.clientId;
    }

    public boolean d() {
        return this.d;
    }

    public boolean e() {
        return this.e;
    }

    public boolean f() {
        return this.f;
    }
}
