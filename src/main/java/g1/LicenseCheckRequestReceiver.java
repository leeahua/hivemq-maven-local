package g1;

import a1.ClusterReceiver;
import aa1.LicenseType;
import ab.ClusterResponse;
import ae1.LicenseInformation;
import af1.LicenseInformationService;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.hivemq.spi.annotations.NotNull;
import d.CacheScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import y1.LicenseCheckRequest;
import y1.LicenseState;

@CacheScoped
public class LicenseCheckRequestReceiver
        implements ClusterReceiver<LicenseCheckRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(LicenseCheckRequestReceiver.class);
    private final LicenseInformationService licenseInformationService;
    
    @Inject
    public LicenseCheckRequestReceiver(LicenseInformationService licenseInformationService) {
        this.licenseInformationService = licenseInformationService;
    }

    public void received(@NotNull LicenseCheckRequest request, @NotNull ClusterResponse response, @NotNull String sender) {
        Preconditions.checkNotNull(request, "Request must not be null");
        Preconditions.checkNotNull(response, "Response must not be null");
        Preconditions.checkNotNull(sender, "Sender must not be null");
        LOGGER.trace("Received license check request from {}.", sender);
        LicenseInformation licenseInformation = this.licenseInformationService.get();
        boolean valid;
        if (licenseInformation.getLicenseType() == LicenseType.NONE) {
            valid = true;
        } else if (licenseInformation.getLicense().isSiteLicense()) {
            valid = true;
        } else if (request.getLicenseState() == LicenseState.NONE) {
            valid = true;
        } else if (request.getLicenseState() == LicenseState.CLUSTER) {
            valid = true;
        } else if (!licenseInformation.getId().equals(request.getLicenseId())) {
            valid = true;
        } else {
            valid = false;
        }
        response.sendResult(valid);
    }
}
