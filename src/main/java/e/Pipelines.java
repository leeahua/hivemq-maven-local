package e;

public class Pipelines {
    public static final String ALL_CHANNEL_GROUP_HANDLER = "all_channel_group_handler";
    public static final String STATISTICS_INITIALIZER = "statistics_initializer";
    public static final String MQTT_MESSAGE_DECODER = "mqtt_message_decoder";
    public static final String GLOBAL_THROTTLING_HANDLER = "global_throttling_handler";
    public static final String CHANNEL_THROTTLING_HANDLER = "channel_throttling_handler";
    public static final String NEW_CONNECTION_IDLE_HANDLER = "new_connection_idle_handler";
    public static final String NO_CONNECT_IDLE_EVENT_HANDLER = "no_connect_idle_event_handler";
    public static final String LICENSE_CONNECTION_LIMITER = "license_connection_limiter";
    public static final String MQTT_LWT_HANDLER = "mqtt_lwt_handler";
    public static final String MQTT_CONNECT_HANDLER = "mqtt_connect_handler";
    public static final String MQTT_CONNACK_HANDLER = "mqtt_connack_handler";
    public static final String MQTT_CONNECT_PERSISTENCE_HANDLER = "mqtt_connect_persistence_handler";
    public static final String MQTT_DISCONNECT_HANDLER = "mqtt_disconnect_handler";
    public static final String MQTT_SUBSCRIBE_HANDLER = "mqtt_subscribe_handler";
    public static final String MQTT_UNSUBSCRIBE_HANDLER = "mqtt_unsubscribe_handler";
    public static final String MQTT_PINGREQ_HANDLER = "mqtt_pingreq_handler";
    public static final String MQTT_QOS_RECEIVER_HANDLER = "mqtt_qos_receiver_handler";
    public static final String MQTT_QOS_SENDER_HANDLER = "mqtt_qos_sender_handler";
    public static final String MQTT_DISALLOW_SECOND_CONNECT = "mqtt_disallow_second_connect";
    public static final String MQTT_KEEPALIVE_IDLE_NOTIFIER_HANDLER = "mqtt_keepalive_idle_notifier_handler";
    public static final String MQTT_KEEPALIVE_IDLE_HANDLER = "mqtt_keepalive_idle_handler";
    public static final String MQTT_PUBLISH_USER_EVENT_HANDLER = "mqtt_publish_user_event_handler";
    public static final String MQTT_PUBLISH_HANDLER = "mqtt_publish_handler";
    public static final String MQTT_ORDERED_TOPIC_HANDLER = "mqtt_ordered_topic_handler";
    public static final String MQTT_MESSAGE_ID_RETURN_HANDLER = "mqtt_message_id_return_handler";
    public static final String HTTP_SERVER_CODEC = "http_server_codec";
    public static final String HTTP_OBJECT_AGGREGATOR = "http_object_aggregator";
    public static final String WEBSOCKET_SERVER_PROTOCOL_HANDLER = "websocket_server_protocol_handler";
    public static final String WEBSOCKET_BINARY_FRAME_HANDLER = "websocket_binary_frame_handler";
    public static final String WEBSOCKET_CONTINUATION_FRAME_HANDLER = "websocket_continuation_frame_handler";
    public static final String WEBSOCKET_TEXT_FRAME_HANDLER = "websocket_text_frame_handler";
    public static final String SSL_EXCEPTION_HANDLER = "ssl_exception_handler";
    public static final String SSL_HANDLER = "ssl_handler";
    public static final String NO_SSL_HANDLER = "no_ssl_handler";
    public static final String SSL_CLIENT_CERTIFICATE_HANDLER = "ssl_client_certificate_handler";
    public static final String CLUSTER_CONNECT_HANDLER = "cluster_connect_handler";
    public static final String MQTT_CLUSTER_PUBLISH_HANDLER = "mqtt_cluster_publish_handler";
    public static final String MQTT_MESSAGE_ENCODER = "mqtt_message_encoder";
    public static final String MQTT_WEBSOCKET_ENCODER = "mqtt_websocket_encoder";
    public static final String GLOBAL_MQTT_MESSAGE_COUNTER = "global_mqtt_message_counter";
    public static final String GLOBAL_TRAFFIC_COUNTER = "global_traffic_counter";
    public static final String MQTT_CONNECTION_COUNTER_HANDLER = "mqtt_connection_counter_handler";
    public static final String MQTT_MESSAGE_BARRIER = "mqtt_message_barrier";
    public static final String MQTT_SUBSCRIBE_MESSAGE_BARRIER = "mqtt_subscribe_message_barrier";
    public static final String PLUGIN_ON_AUTHENTICATION_CALLBACK_HANDLER = "plugin_on_authentication_callback_handler";
    public static final String PLUGIN_AFTER_LOGIN_CALLBACK_HANDLER = "plugin_after_login_callback_handler";
    public static final String PLUGIN_RESTRICTIONS_CALLBACK_HANDLER = "plugin_restrictions_callback_handler";
    public static final String PLUGIN_ON_CONNECT_CALLBACK_HANDLER = "plugin_on_connect_callback_handler";
    public static final String EXCEPTION_HANDLER = "exception_handler";
}