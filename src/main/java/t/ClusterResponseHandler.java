package t;

public interface ClusterResponseHandler<S> {
    void handle(String node, S result);
}
