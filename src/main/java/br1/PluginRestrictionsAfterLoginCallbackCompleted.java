package br1;

import com.hivemq.spi.message.Connect;
import com.hivemq.spi.security.ClientCredentials;
import com.hivemq.spi.security.Restriction;

import java.util.Set;

public class PluginRestrictionsAfterLoginCallbackCompleted {
    private final ClientCredentials clientCredentials;
    private final Connect connect;
    private final Set<Restriction> restrictions;

    public PluginRestrictionsAfterLoginCallbackCompleted(ClientCredentials clientCredentials,
                                                         Connect connect,
                                                         Set<Restriction> restrictions) {
        this.clientCredentials = clientCredentials;
        this.connect = connect;
        this.restrictions = restrictions;
    }

    public ClientCredentials getClientCredentials() {
        return clientCredentials;
    }

    public Connect getConnect() {
        return connect;
    }

    public Set<Restriction> getRestrictions() {
        return restrictions;
    }
}
