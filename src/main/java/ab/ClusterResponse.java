package ab;

import ac.SerializationService;
import org.jgroups.blocks.Response;
import t.ClusterConnection;

public class ClusterResponse {
    private final SerializationService serializationService;
    private final ClusterConnection clusterConnection;
    private final Response response;

    public ClusterResponse(SerializationService serializationService,
                           ClusterConnection clusterConnection,
                           Response response) {
        this.serializationService = serializationService;
        this.clusterConnection = clusterConnection;
        this.response = response;
    }

    public <R> void sendResult(ClusterResponseCode code, R result, boolean validLicense) {
        if (this.clusterConnection.getJChannel().isConnected() &&
                !this.clusterConnection.getJChannel().isClosed()) {
            this.response.send(this.serializationService.serialize(new ClusterResult(code, result, validLicense)), false);
        }
    }

    public <R> void sendResult(ClusterResponseCode code, R result) {
        if (this.clusterConnection.getJChannel().isConnected() &&
                !this.clusterConnection.getJChannel().isClosed()) {
            this.response.send(this.serializationService.serialize(new ClusterResult(code, result)), false);
        }
    }

    public void sendResult(ClusterResponseCode code) {
        if (this.clusterConnection.getJChannel().isConnected() &&
                !this.clusterConnection.getJChannel().isClosed()) {
            this.response.send(this.serializationService.serialize(new ClusterResult(code, null)), false);
        }
    }

    public <R> void sendResult(R result) {
        if (this.clusterConnection.getJChannel().isConnected() &&
                !this.clusterConnection.getJChannel().isClosed()) {
            this.response.send(this.serializationService.serialize(new ClusterResult(ClusterResponseCode.OK, result)), false);
        }
    }

    public void sendResult() {
        if (this.clusterConnection.getJChannel().isConnected() &&
                !this.clusterConnection.getJChannel().isClosed()) {
            this.response.send(this.serializationService.ok(), false);
        }
    }
}
