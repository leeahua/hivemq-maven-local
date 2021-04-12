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
import y1.LicenseInformationRequest;
import y1.LicenseInformationResult;
import y1.LicenseState;

@CacheScoped
public class LicenseInformationRequestReceiver
        implements ClusterReceiver<LicenseInformationRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(LicenseInformationRequestReceiver.class);
    private final LicenseInformationService licenseInformationService;

    @Inject
    public LicenseInformationRequestReceiver(LicenseInformationService licenseInformationService) {
        this.licenseInformationService = licenseInformationService;
    }

    public void received(@NotNull LicenseInformationRequest request, @NotNull ClusterResponse response, @NotNull String sender) {
        Preconditions.checkNotNull(request, "Request must not be null");
        Preconditions.checkNotNull(response, "Response must not be null");
        Preconditions.checkNotNull(sender, "Sender must not be null");
        LOGGER.trace("Received license information request from {}.", sender);
        LicenseInformation licenseInformation = this.licenseInformationService.get();
        LicenseState licenseState;
        if (licenseInformation.getLicenseType() == LicenseType.NONE) {
            licenseState = LicenseState.NONE;
        } else if (licenseInformation.getLicense().isSiteLicense()) {
            licenseState = LicenseState.CLUSTER;
        } else {
            licenseState = LicenseState.SINGLE;
        }
        LicenseInformationResult result;
        if (licenseState == LicenseState.NONE) {
            result = new LicenseInformationResult(licenseState, null);
        } else {
            result = new LicenseInformationResult(licenseState, licenseInformation.getId());
        }
        response.sendResult(result);
    }
}
