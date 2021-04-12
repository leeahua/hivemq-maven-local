package u;

import aj.ClusterFutures;
import av.InternalConfigurationService;
import av.Internals;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableScheduledFuture;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import s.Cluster;
import v.ClientSessionClusterPersistence;
import w.QueuedMessagesClusterPersistence;
import x.RetainedMessagesClusterPersistence;
import y.ClientSessionSubscriptionsClusterPersistence;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class PersistenceCleanupService {
    private static final Logger LOGGER = LoggerFactory.getLogger(PersistenceCleanupService.class);
    private final ListeningScheduledExecutorService clusterScheduledExecutorService;
    private final InternalConfigurationService internalConfigurationService;
    private final ClientSessionClusterPersistence clientSessionClusterPersistence;
    private final ClientSessionSubscriptionsClusterPersistence clientSessionSubscriptionsClusterPersistence;
    private final QueuedMessagesClusterPersistence queuedMessagesClusterPersistence;
    private final RetainedMessagesClusterPersistence retainedMessagesClusterPersistence;

    @Inject
    public PersistenceCleanupService(@Cluster ListeningScheduledExecutorService clusterScheduledExecutorService,
                                     InternalConfigurationService internalConfigurationService,
                                     ClientSessionClusterPersistence clientSessionClusterPersistence,
                                     ClientSessionSubscriptionsClusterPersistence clientSessionSubscriptionsClusterPersistence,
                                     QueuedMessagesClusterPersistence queuedMessagesClusterPersistence,
                                     RetainedMessagesClusterPersistence retainedMessagesClusterPersistence) {
        this.clusterScheduledExecutorService = clusterScheduledExecutorService;
        this.internalConfigurationService = internalConfigurationService;
        this.clientSessionClusterPersistence = clientSessionClusterPersistence;
        this.clientSessionSubscriptionsClusterPersistence = clientSessionSubscriptionsClusterPersistence;
        this.queuedMessagesClusterPersistence = queuedMessagesClusterPersistence;
        this.retainedMessagesClusterPersistence = retainedMessagesClusterPersistence;
    }

    @PostConstruct
    void init() {
        scheduleCleanUpJob();
    }

    private void scheduleCleanUpJob() {
        long cleanupJobSchedule = this.internalConfigurationService.getLong(Internals.CLEANUP_JOB_SCHEDULE);
        long tombstoneMaxAge = this.internalConfigurationService.getLong(Internals.TOMBSTONE_MAX_AGE);
        ListenableScheduledFuture scheduledFuture =
                this.clusterScheduledExecutorService.schedule(
                        new CleanUpJob(this, tombstoneMaxAge), cleanupJobSchedule, TimeUnit.SECONDS);
        ClusterFutures.waitFuture(scheduledFuture);
    }

    public ListenableFuture<Void> cleanUp(long tombstoneMaxAge) {
        List<ListenableFuture<Void>> futures = new ArrayList<>();
        futures.add(this.clientSessionClusterPersistence.cleanUp(tombstoneMaxAge));
        futures.add(this.clientSessionSubscriptionsClusterPersistence.cleanUp(tombstoneMaxAge));
        futures.add(this.queuedMessagesClusterPersistence.cleanUp(tombstoneMaxAge));
        futures.add(this.retainedMessagesClusterPersistence.cleanUp(tombstoneMaxAge));
        return ClusterFutures.merge(futures);
    }


    private static final class CleanUpJob implements Runnable {
        private final PersistenceCleanupService cleanupService;
        private final long tombstoneMaxAge;

        private CleanUpJob(PersistenceCleanupService cleanupService, long tombstoneMaxAge) {
            this.cleanupService = cleanupService;
            this.tombstoneMaxAge = tombstoneMaxAge;
        }

        public void run() {
            ListenableFuture<Void> future = this.cleanupService.cleanUp(this.tombstoneMaxAge * 1000L);
            ClusterFutures.addCallback(future, new FutureCallback<Void>() {

                @Override
                public void onSuccess(@Nullable Void result) {
                    cleanupService.scheduleCleanUpJob();
                }

                @Override
                public void onFailure(Throwable t) {
                    LOGGER.error("Exception during cleanup.", t);
                    cleanupService.scheduleCleanUpJob();
                }
            });
        }
    }
}
