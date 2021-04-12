package t;

import j1.ClusterRequest;
import k1.ClusterCallback;

public interface ClusterCallbackFactory<S, Q extends ClusterRequest> {
    ClusterCallback<S, Q> create();
}
