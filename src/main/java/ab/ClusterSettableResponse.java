package ab;

import ac.SerializationService;
import com.google.common.util.concurrent.SettableFuture;
import org.jgroups.blocks.Response;
import t.ClusterConnection;

public class ClusterSettableResponse<S> extends ClusterResponse {
    private final SettableFuture<S> settableFuture;

    public ClusterSettableResponse(SerializationService serializationService,
                                   ClusterConnection clusterConnection,
                                   Response response,
                                   SettableFuture<S> settableFuture) {
        super(serializationService, clusterConnection, response);
        this.settableFuture = settableFuture;
    }

    public <R> void sendResult(ClusterResponseCode code, R result) {
        if (code != ClusterResponseCode.OK) {
            this.settableFuture.setException(code.createException());
        } else {
            this.settableFuture.set((S)result);
        }
    }

    public void sendResult(ClusterResponseCode code) {
        if (code != ClusterResponseCode.OK) {
            this.settableFuture.setException(code.createException());
        } else {
            this.settableFuture.set(null);
        }
    }

    public <R> void sendResult(R result) {
        this.settableFuture.set((S)result);
    }

    public void sendResult() {
        this.settableFuture.set(null);
    }
}
