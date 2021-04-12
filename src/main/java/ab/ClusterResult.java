package ab;

import ah.ClusterState;
import j1.ClusterRequest;
import p.ClusterRequestException;
import p.DifferentConfigurationException;
import p.DuplicateOrInvalidLicenseException;
import y1.ClusterStateRequest;

public class ClusterResult<T> {
    private final ClusterResponseCode code;
    private final T data;
    private final boolean validLicense;

    public ClusterResult(ClusterResponseCode code, T data) {
        this.code = code;
        this.data = data;
        this.validLicense = true;
    }

    public ClusterResult(ClusterResponseCode code, T data, boolean validLicense) {
        this.code = code;
        this.data = data;
        this.validLicense = validLicense;
    }

    public ClusterResponseCode getCode() {
        return code;
    }

    public T getData() {
        return data;
    }

    public boolean isValidLicense() {
        return validLicense;
    }

    public static Throwable createException(ClusterRequest request, boolean validLicense, ClusterResponseCode code) {
        if (request instanceof ClusterStateRequest) {
            ClusterStateRequest clusterStateRequest = (ClusterStateRequest) request;
            if (code == ClusterResponseCode.FAILED && !validLicense) {
                return new DuplicateOrInvalidLicenseException();
            }
            if (clusterStateRequest.getState() == ClusterState.NOT_JOINED && !validLicense) {
                return new DifferentConfigurationException();
            }
        }
        throw new ClusterRequestException();
    }
}
