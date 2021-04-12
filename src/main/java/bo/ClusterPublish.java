package bo;

import bu.InternalPublish;
import com.google.common.util.concurrent.SettableFuture;

public class ClusterPublish extends InternalPublish {
    private final SettableFuture<SendStatus> settableFuture;

    public ClusterPublish(InternalPublish publish,
                          SettableFuture<SendStatus> settableFuture) {
        super(publish);
        this.settableFuture = settableFuture;
    }

    public SettableFuture<SendStatus> getSettableFuture() {
        return settableFuture;
    }
}
