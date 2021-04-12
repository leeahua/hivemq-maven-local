package br1;

import com.hivemq.spi.message.Connect;
import com.hivemq.spi.security.ClientCredentials;
import com.hivemq.spi.security.Restriction;

public class PluginRestrictionsAfterLoginCompleted {
    private final Connect connect;
    private final ClientCredentials clientCredentials;
    private final Iterable<Restriction> restrictions;

    public PluginRestrictionsAfterLoginCompleted(Connect connect,
                                                 ClientCredentials clientCredentials,
                                                 Iterable<Restriction> restrictions) {
        this.connect = connect;
        this.clientCredentials = clientCredentials;
        this.restrictions = restrictions;
    }

    public Connect getConnect() {
        return connect;
    }

    public ClientCredentials getClientCredentials() {
        return clientCredentials;
    }

    public Iterable<Restriction> getRestrictions() {
        return restrictions;
    }
}
