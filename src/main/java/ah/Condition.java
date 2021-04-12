package ah;

import com.google.common.base.Preconditions;
import com.hivemq.spi.annotations.NotNull;

import java.util.Map;

public abstract class Condition {
    public abstract boolean test(Map<String, ClusterState> clusterNodeStates);

    public static Condition allEqual(ClusterState state) {
        return new AllEqual(state);
    }

    public static Condition allNotEqual(ClusterState state) {
        return new AllNotEqual(state);
    }

    public Condition and(Condition condition) {
        return new AllMatch(this, condition);
    }


    static class AllEqual extends Condition {
        private final ClusterState state;

        AllEqual(ClusterState state) {
            this.state = state;
        }

        public boolean test(Map<String, ClusterState> clusterNodeStates) {
            return clusterNodeStates.values().stream()
                    .allMatch(state -> state == this.state);
        }
    }

    static class AllNotEqual
            extends Condition {
        private final ClusterState state;

        AllNotEqual(ClusterState state) {
            this.state = state;
        }

        public boolean test(Map<String, ClusterState> clusterNodeStates) {
            return clusterNodeStates.values().stream()
                    .allMatch(state -> state != this.state);
        }
    }


    static class AnyMatch
            extends Condition {
        private final Condition condition1;
        private final Condition condition2;

        public AnyMatch(@NotNull Condition condition1, @NotNull Condition condition2) {
            Preconditions.checkNotNull(condition1, "Condition must not be null");
            Preconditions.checkNotNull(condition2, "Condition must not be null");
            this.condition1 = condition1;
            this.condition2 = condition2;
        }

        public boolean test(Map<String, ClusterState> clusterNodeStates) {
            return this.condition1.test(clusterNodeStates) ||
                    this.condition2.test(clusterNodeStates);
        }
    }


    static class AllMatch
            extends Condition {
        private final Condition condition1;
        private final Condition condition2;

        public AllMatch(@NotNull Condition condition1, @NotNull Condition condition2) {
            Preconditions.checkNotNull(condition1, "Condition must not be null");
            Preconditions.checkNotNull(condition2, "Condition must not be null");
            this.condition1 = condition1;
            this.condition2 = condition2;
        }

        public boolean test(Map<String, ClusterState> clusterNodeStates) {
            return this.condition1.test(clusterNodeStates) &&
                    this.condition2.test(clusterNodeStates);
        }
    }
}
