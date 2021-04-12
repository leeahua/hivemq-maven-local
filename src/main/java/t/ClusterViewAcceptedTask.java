package t;

import ah.ClusterService;
import org.jgroups.Address;
import org.jgroups.MergeView;
import org.jgroups.View;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

public class ClusterViewAcceptedTask implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterViewAcceptedTask.class);
    private final View oldView;
    private final View newView;
    private final ClusterService clusterService;

    public ClusterViewAcceptedTask(View oldView, View newView, ClusterService clusterService) {
        this.oldView = oldView;
        this.newView = newView;
        this.clusterService = clusterService;
    }

    public void run() {
        if (this.newView instanceof MergeView) {
            MergeView mergeView = (MergeView) this.newView;
            LOGGER.debug("Merging Groups: {}.", mergeView.getSubgroups());
            Address coordinator = mergeView.getMembers().get(0);
            Set<Address> members = new HashSet<>();
            mergeView.getSubgroups().forEach(subgroup -> {
                if (subgroup.containsMember(coordinator)) {
                    LOGGER.debug("Primary Group: {}.", subgroup.getMembers());
                    return;
                }
                members.addAll(subgroup.getMembers());
            });
            LOGGER.debug("Minority Group: {}.", members);
            this.clusterService.onMerge(members);
            return;
        }

        this.newView.getMembers().stream()
                .filter(member -> this.oldView == null || !this.oldView.containsMember(member))
                .forEach(this.clusterService::connected);
        if (this.oldView == null) {
            return;
        }
        this.oldView.getMembers().stream()
                .filter(member -> !this.newView.containsMember(member))
                .forEach(this.clusterService::disconnected);
    }
}
