package cb1;

import com.hivemq.spi.message.ProtocolVersion;
import com.hivemq.spi.security.SslClientCertificate;
import io.netty.util.AttributeKey;

public class AttributeKeys {
    public static final AttributeKey<ProtocolVersion> MQTT_VERSION = AttributeKey.valueOf("MQTT.Version");
    public static final AttributeKey<String> MQTT_CLIENT_ID = AttributeKey.valueOf("MQTT.ClientId");
    public static final AttributeKey<Boolean> MQTT_PERSISTENT_SESSION = AttributeKey.valueOf("MQTT.PersistentSession");
    public static final AttributeKey<Boolean> MQTT_CONNACK_SENT = AttributeKey.valueOf("MQTT.ConnackSent");
    public static final AttributeKey<Boolean> MQTT_TAKEN_OVER = AttributeKey.valueOf("MQTT.TakenOver");
    public static final AttributeKey<SslClientCertificate> AUTH_CERTIFICATE = AttributeKey.valueOf("Auth.Certificate");
    public static final AttributeKey<String> AUTH_USERNAME = AttributeKey.valueOf("Auth.Username");
    public static final AttributeKey<byte[]> AUTH_PASSWORD = AttributeKey.valueOf("Auth.Password");
    public static final AttributeKey<Boolean> AUTHENTICATED = AttributeKey.valueOf("Authenticated");
    public static final AttributeKey<Boolean> CLUSTER_LOCAL_CONNECTION = AttributeKey.valueOf("Cluster.LocalConnection");
    public static final AttributeKey<Long> RESTRICTION_MAX_PUBLISH_MESSAGE_SIZE = AttributeKey.valueOf("Restriction.MaxPublishMessageSize");
    public static final AttributeKey<Long> RESTRICTION_MAX_OUTGOING_BYTES_SEC = AttributeKey.valueOf("Restriction.MaxOutgoingBytesSec");
    public static final AttributeKey<Long> RESTRICTION_MAX_INCOMING_BYTES_SEC = AttributeKey.valueOf("Restriction.MaxIncomingBytesSec");
}
