package i;

import aj.ClusterFutures;
import bo.ClusterPublishResult;
import bo.SendStatus;
import bu.InternalPublish;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.SettableFuture;
import com.google.inject.Inject;
import com.hivemq.spi.annotations.NotNull;
import com.hivemq.spi.message.PubRel;
import d.CacheScoped;
import j1.ClusterRequestFuture;
import o1.ClusterPubRelRequestCallback;
import o1.ClusterPublishRequestCallback;
import s.Cluster;
import t.ClusterConnection;
import v.ClientSessionClusterPersistence;
import w1.ClusterPubRelRequest;
import w1.ClusterPublishRequest;

import javax.annotation.Nullable;
import java.util.concurrent.ExecutorService;
// TODO:
@CacheScoped
public class PublishDispatcherImpl implements PublishDispatcher {
    private final ClusterConnection clusterConnection;
    private final ClientSessionClusterPersistence clientSessionClusterPersistence;
    private final ListeningExecutorService clusterExecutor;

    @Inject
    PublishDispatcherImpl(ClusterConnection clusterConnection,
                          ClientSessionClusterPersistence clientSessionClusterPersistence,
                          @Cluster ListeningExecutorService clusterExecutor) {
        this.clusterConnection = clusterConnection;
        this.clientSessionClusterPersistence = clientSessionClusterPersistence;
        this.clusterExecutor = clusterExecutor;
    }

    public ListenableFuture<ClusterPublishResult> dispatch(
            @NotNull InternalPublish publish,
            @NotNull String clientId,
            int qoSNumber,
            boolean paramBoolean1,
            boolean paramBoolean2,
            boolean paramBoolean3) {
        return dispatch(publish, clientId, qoSNumber, paramBoolean1, paramBoolean2, paramBoolean3, this.clusterExecutor);
    }

    public ListenableFuture<ClusterPublishResult> dispatch(
            @NotNull InternalPublish publish,
            @NotNull String clientId,
            int qoSNumber,
            boolean paramBoolean1,
            boolean paramBoolean2,
            boolean paramBoolean3,
            ExecutorService executorService) {
        Preconditions.checkNotNull(publish, "Publish must not be null");
        Preconditions.checkNotNull(clientId, "Client id must not be null");
        SettableFuture<ClusterPublishResult> settableFuture = SettableFuture.create();
        ListenableFuture<String> nodeForPublishFuture = this.clientSessionClusterPersistence.nodeForPublish(clientId, executorService);
        Futures.addCallback(nodeForPublishFuture, new FutureCallback<String>() {

            @Override
            public void onSuccess(@Nullable String result) {
                ListenableFuture<SendStatus> rf = a(publish, qoSNumber, clientId, result, paramBoolean1, paramBoolean2, paramBoolean3);
                Futures.addCallback(rf, new FutureCallback<SendStatus>() {

                    @Override
                    public void onSuccess(@Nullable SendStatus result) {
                        settableFuture.set(new ClusterPublishResult(result, clientId));
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        settableFuture.setException(t);
                    }
                }, executorService);
            }

            @Override
            public void onFailure(Throwable t) {
                settableFuture.setException(t);
            }
        }, executorService);
        return settableFuture;
    }

    public ListenableFuture<SendStatus> b(InternalPublish publish,
                                          String clientId,
                                          int qoSNumber,
                                          boolean paramBoolean1,
                                          boolean paramBoolean2,
                                          boolean paramBoolean3) {
        SettableFuture<SendStatus> settableFuture = SettableFuture.create();
        ListenableFuture<String> nodeForPublishFuture =
                this.clientSessionClusterPersistence.nodeForPublish(clientId, this.clusterExecutor);
        ClusterFutures.addCallback(nodeForPublishFuture, new FutureCallback<String>() {

            @Override
            public void onSuccess(@Nullable String result) {
                settableFuture.setFuture(a(publish, qoSNumber, clientId, result, paramBoolean1, paramBoolean2, paramBoolean3));
            }

            @Override
            public void onFailure(Throwable t) {
                settableFuture.setException(t);
            }
        });
        return settableFuture;
    }

    public ListenableFuture<Void> dispatch(@NotNull PubRel pubRel,
                                           @NotNull String clientId) {
        Preconditions.checkNotNull(pubRel, "Publish must not be null");
        Preconditions.checkNotNull(clientId, "Client id must not be null");
        SettableFuture<Void> settableFuture = SettableFuture.create();
        ListenableFuture<String> nodeForPublishFuture = this.clientSessionClusterPersistence.nodeForPublish(clientId, this.clusterExecutor);
        ClusterFutures.addCallback(nodeForPublishFuture, new FutureCallback<String>() {

            @Override
            public void onSuccess(@Nullable String result) {
                ClusterPubRelRequest request = new ClusterPubRelRequest(pubRel, clientId);
                ClusterRequestFuture requestFuture = clusterConnection.send(request, result, Void.class);
                settableFuture.setFuture(requestFuture.setCallback(
                        new ClusterPubRelRequestCallback(PublishDispatcherImpl.this)));
            }

            @Override
            public void onFailure(Throwable t) {
                settableFuture.setException(t);
            }
        });
        return settableFuture;
    }

    private ListenableFuture<SendStatus> a(InternalPublish publish,
                                           int qoSNumber,
                                           String clientId,
                                           String nodeForPublish,
                                           boolean paramBoolean1,
                                           boolean paramBoolean2,
                                           boolean paramBoolean3) {
        ClusterPublishRequest request = new ClusterPublishRequest(publish, qoSNumber,
                clientId, paramBoolean1, paramBoolean2, paramBoolean3);
        ClusterRequestFuture<SendStatus, ClusterPublishRequest> requestFuture =
                this.clusterConnection.send(request, nodeForPublish, SendStatus.class);
        return requestFuture.setCallback(new ClusterPublishRequestCallback(this));
    }
}
