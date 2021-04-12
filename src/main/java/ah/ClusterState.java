package ah;

public enum ClusterState {
    UNKNOWN(0),              //a
    NOT_JOINED(1),          //b
    JOINING(2),             //c
    RUNNING(3),              //d
    MERGE_MINORITY(4),      //e
    MERGING(5),             //f
    SHUTTING_DOWN(6),       //g
    SHUTDOWN_FINISHED(7),   //h
    DEAD(8);                //i

    private final int value;

    ClusterState(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static ClusterState valueOf(int value) {
        for (ClusterState state : values()) {
            if (state.getValue() == value) {
                return state;
            }
        }
        throw new IllegalArgumentException("No state found for the given value : " + value + ".");
    }
}
