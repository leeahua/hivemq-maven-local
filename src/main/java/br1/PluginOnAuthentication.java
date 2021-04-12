package br1;

import com.hivemq.spi.message.Connect;
import com.hivemq.spi.security.ClientCredentials;

public class PluginOnAuthentication {
    private final Connect connect;
    private final ClientCredentials clientCredentials;

    public PluginOnAuthentication(Connect connect, ClientCredentials clientCredentials) {
        this.connect = connect;
        this.clientCredentials = clientCredentials;
    }

    public Connect getConnect() {
        return connect;
    }

    public ClientCredentials getClientCredentials() {
        return clientCredentials;
    }
}
