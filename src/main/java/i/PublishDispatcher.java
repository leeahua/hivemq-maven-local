package i;

import bo.ClusterPublishResult;
import bu.InternalPublish;
import com.google.common.util.concurrent.ListenableFuture;
import com.hivemq.spi.annotations.NotNull;
import com.hivemq.spi.message.PubRel;

import java.util.concurrent.ExecutorService;
// TODO:
public interface PublishDispatcher {

    ListenableFuture<ClusterPublishResult> dispatch(@NotNull InternalPublish publish, @NotNull String clientId,
                                                    int qoSNumber, boolean paramBoolean1, boolean paramBoolean2,
                                                    boolean paramBoolean3);

    ListenableFuture<ClusterPublishResult> dispatch(@NotNull InternalPublish publish, @NotNull String clientId,
                                                    int qoSNumber, boolean paramBoolean1, boolean paramBoolean2,
                                                    boolean paramBoolean3, ExecutorService executorService);

    ListenableFuture<Void> dispatch(@NotNull PubRel pubRel, @NotNull String clientId);
}
