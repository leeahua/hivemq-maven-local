package cb1;

import com.hivemq.spi.annotations.NotNull;
import com.hivemq.spi.security.SslClientCertificate;
import cs.ClientToken;
import io.netty.channel.Channel;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Optional;

public class ChannelUtils {

    public static Optional<String> remoteIP(Channel channel) {
        Optional<InetAddress> mayRemoteAddress = remoteAddress(channel);
        if (!mayRemoteAddress.isPresent()) {
            return Optional.empty();
        }
        return Optional.ofNullable(mayRemoteAddress.get().getHostAddress());
    }

    public static Optional<InetAddress> remoteAddress(Channel channel) {
        Optional<SocketAddress> maySocketAddress = Optional.ofNullable(channel.remoteAddress());
        if (!maySocketAddress.isPresent()) {
            return Optional.empty();
        }
        SocketAddress socketAddress = maySocketAddress.get();
        if (socketAddress instanceof InetSocketAddress) {
            return Optional.ofNullable(((InetSocketAddress) socketAddress).getAddress());
        }
        return Optional.empty();
    }


    public static ClientToken clientToken(@NotNull Channel channel) {
        String clientId = channel.attr(AttributeKeys.MQTT_CLIENT_ID).get();
        String username = channel.attr(AttributeKeys.AUTH_USERNAME).get();
        byte[] password = channel.attr(AttributeKeys.AUTH_PASSWORD).get();
        SslClientCertificate certificate = channel.attr(AttributeKeys.AUTH_CERTIFICATE).get();
        ClientToken clientToken = new ClientToken(clientId, username, password,
                certificate, false, remoteAddress(channel).orElse(null));
        Boolean authenticated = channel.attr(AttributeKeys.AUTHENTICATED).get();
        clientToken.setAuthenticated(authenticated != null ? authenticated.booleanValue() : false);
        return clientToken;
    }
}
