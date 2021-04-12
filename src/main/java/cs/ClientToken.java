package cs;

import com.google.common.base.Preconditions;
import com.hivemq.spi.annotations.NotNull;
import com.hivemq.spi.annotations.Nullable;
import com.hivemq.spi.security.ClientCredentials;
import com.hivemq.spi.security.SslClientCertificate;

import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;

public class ClientToken implements ClientCredentials {
    private final boolean bridge;
    private String clientId;
    private Optional<String> username;
    private Optional<byte[]> password;
    private Optional<SslClientCertificate> certificate;
    private boolean authenticated = false;
    private boolean anonymous = true;
    private Optional<InetAddress> inetAddress;

    public ClientToken(@NotNull String clientId,
                       @Nullable String username,
                       @Nullable byte[] password,
                       @Nullable SslClientCertificate certificate,
                       boolean bridge,
                       @Nullable InetAddress inetAddress) {
        this.clientId = Preconditions.checkNotNull(clientId, "client identifier must not be null");
        this.username = Optional.ofNullable(username);
        this.password = Optional.ofNullable(password);
        this.certificate = Optional.ofNullable(certificate);
        this.inetAddress = Optional.ofNullable(inetAddress);
        this.bridge = bridge;
    }

    public ClientToken(@NotNull String clientId,
                       @Nullable String username,
                       @Nullable byte[] password,
                       @Nullable SslClientCertificate certificate,
                       @Nullable InetAddress inetAddress) {
        this(clientId, username, password, certificate, false, inetAddress);
    }

    public ClientToken(@NotNull String clientId,
                       @Nullable String username,
                       @Nullable byte[] password,
                       @Nullable InetAddress inetAddress) {
        this(clientId, username, password, null, false, inetAddress);
    }

    public ClientToken(@NotNull String clientId,
                       @Nullable InetAddress inetAddress) {
        this(clientId, null, null, null, false, inetAddress);
    }

    public ClientToken(@NotNull String clientId) {
        this(clientId, null, null, null, false, null);
    }

    public Optional<SslClientCertificate> getCertificate() {
        return this.certificate;
    }

    public boolean isAnonymous() {
        return this.anonymous;
    }

    public boolean isBridge() {
        return this.bridge;
    }

    public Optional<InetAddress> getInetAddress() {
        return this.inetAddress;
    }

    public String getClientId() {
        return this.clientId;
    }

    public Optional<String> getPassword() {
        return this.password.filter(Objects::nonNull)
                .map(p -> new String(p, StandardCharsets.UTF_8));
    }

    public Optional<byte[]> getPasswordBytes() {
        return this.password;
    }

    public Optional<String> getUsername() {
        return this.username;
    }

    public boolean isAuthenticated() {
        return this.authenticated;
    }

    public void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
        this.anonymous = (!authenticated);
    }

    public void clear() {
        this.username = Optional.empty();
        this.clientId = null;
        this.password = Optional.empty();
        this.certificate = Optional.empty();
        this.authenticated = false;
        this.inetAddress = Optional.empty();
        this.anonymous = true;
    }

    public String toString() {
        return "ClientToken{bridge=" + this.bridge +
                ", clientId='" + this.clientId + '\'' +
                ", username=" + (this.username.isPresent() ? this.username.get() : "null") +
                ", password=" + (this.password.isPresent() ? "********" : "null") +
                ", certificate=" + (this.certificate.isPresent() ? "present" : "null") +
                ", isAuthenticated=" + this.authenticated +
                ", isAnonymous=" + this.anonymous +
                ", inetAddress=" + (this.inetAddress.isPresent() ? (this.inetAddress.get()).getHostAddress() : "null") +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ClientToken that = (ClientToken) o;
        return bridge == that.bridge &&
                authenticated == that.authenticated &&
                anonymous == that.anonymous &&
                Objects.equals(clientId, that.clientId) &&
                Objects.equals(username, that.username) &&
                Objects.equals(password, that.password) &&
                Objects.equals(certificate, that.certificate) &&
                Objects.equals(inetAddress, that.inetAddress);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bridge, clientId, username, password, certificate, authenticated, anonymous, inetAddress);
    }
}
