package br1;

import ch.PluginOnAuthenticationResult;
import com.hivemq.spi.callback.security.OnAuthenticationCallback;
import com.hivemq.spi.message.Connect;
import com.hivemq.spi.security.ClientCredentials;

import java.util.List;
import java.util.Queue;

public class PluginOnAuthenticationCallbackCompleted {
    private final Queue<OnAuthenticationCallback> leftCallbacks;
    private final int expectedResultCount;
    private final List<PluginOnAuthenticationResult> results;
    private final Connect connect;
    private final ClientCredentials clientCredentials;

    public PluginOnAuthenticationCallbackCompleted(Queue<OnAuthenticationCallback> leftCallbacks,
                                                   List<PluginOnAuthenticationResult> results,
                                                   Connect connect,
                                                   ClientCredentials clientCredentials,
                                                   int expectedResultCount) {
        this.leftCallbacks = leftCallbacks;
        this.results = results;
        this.connect = connect;
        this.clientCredentials = clientCredentials;
        this.expectedResultCount = expectedResultCount;
    }

    public Queue<OnAuthenticationCallback> getLeftCallbacks() {
        return leftCallbacks;
    }

    public int getExpectedResultCount() {
        return expectedResultCount;
    }

    public List<PluginOnAuthenticationResult> getResults() {
        return results;
    }

    public Connect getConnect() {
        return connect;
    }

    public ClientCredentials getClientCredentials() {
        return clientCredentials;
    }
}
