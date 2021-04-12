package k1;

import j1.ClusterRequest;
import t.ClusterCallbackFactory;

public class ClusterCallbackFactoryImpl<S, Q extends ClusterRequest> implements ClusterCallbackFactory<S, Q> {
    private final Class<S> resultType;

    public ClusterCallbackFactoryImpl(Class<S> resultType) {
        this.resultType = resultType;
    }

    public ClusterCallback<S, Q> create() {
        return new DefaultClusterCallback(this.resultType);
    }
}
