package a1;

import ab.ClusterResponse;
import ab.ClusterResponseCode;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import i.ClusterIdProducer;
import q.ConsistentHashingRing;

import javax.annotation.Nullable;
import java.util.concurrent.ExecutorService;

public abstract class GenericRequestReceiver extends AbstractRequestReceiver {
    private final ExecutorService rpcExecutor;

    protected GenericRequestReceiver(ConsistentHashingRing primaryRing,
                                     ConsistentHashingRing minorityRing,
                                     ClusterIdProducer clusterIdProducer,
                                     int replicateCount,
                                     ExecutorService rpcExecutor) {
        super(primaryRing, minorityRing, clusterIdProducer, replicateCount);
        this.rpcExecutor = rpcExecutor;
    }

    protected void sendResult(ListenableFuture<Void> future, ClusterResponse response) {
        Futures.addCallback(future, new FutureCallback<Void>() {

            @Override
            public void onSuccess(@Nullable Void result) {
                response.sendResult();
            }

            @Override
            public void onFailure(Throwable t) {
                GenericRequestReceiver.this.onFailure(t);
                response.sendResult(ClusterResponseCode.FAILED);
            }
        }, this.rpcExecutor);
    }

    protected abstract void onFailure(Throwable t);
}
