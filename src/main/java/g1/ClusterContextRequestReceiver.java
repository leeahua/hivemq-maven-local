package g1;

import a1.ClusterReceiver;
import ab.ClusterResponse;
import ah.ClusterContext;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.hivemq.spi.annotations.NotNull;
import com.hivemq.spi.config.SystemInformation;
import d.CacheScoped;
import i.ClusterConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import y1.ClusterContextRequest;

@CacheScoped
public class ClusterContextRequestReceiver
        implements ClusterReceiver<ClusterContextRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterContextRequestReceiver.class);
    private final ClusterConfigurationService clusterConfigurationService;
    private final SystemInformation systemInformation;
    private final ClusterContext clusterContext;

    @Inject
    public ClusterContextRequestReceiver(
            ClusterConfigurationService clusterConfigurationService,
            SystemInformation systemInformation,
            ClusterContext clusterContext) {
        this.clusterConfigurationService = clusterConfigurationService;
        this.systemInformation = systemInformation;
        this.clusterContext = clusterContext;
    }

    public void received(@NotNull ClusterContextRequest request, @NotNull ClusterResponse response, @NotNull String sender) {
        Preconditions.checkNotNull(request, "Request must not be null");
        Preconditions.checkNotNull(response, "Response must not be null");
        Preconditions.checkNotNull(sender, "Sender must not be null");
        LOGGER.trace("Receiver cluster context request.");
        ImmutableMap.Builder<String, Object> builder = ImmutableMap.builder();
        builder.put("transport.type", this.clusterConfigurationService.getTransport().getType());
        builder.put("discovery.type", this.clusterConfigurationService.getDiscovery().getType());
        builder.put("hivemq.version", this.systemInformation.getHiveMQVersion());
        request.getNodeInformations().forEach(
                (node, version) -> this.clusterContext.setVersion(node, version));
        response.sendResult(builder.build());
    }
}
