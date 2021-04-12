package ci;

import com.hivemq.spi.callback.security.authorization.AuthorizationBehaviour;
import com.hivemq.spi.callback.security.authorization.AuthorizationResult;
import com.hivemq.spi.topic.MqttTopicPermission;

import java.util.List;

public class AuthorizationResultImpl implements AuthorizationResult {
    private final List<MqttTopicPermission> permissions;
    private final AuthorizationBehaviour behaviour;

    public AuthorizationResultImpl(List<MqttTopicPermission> permissions,
                                   AuthorizationBehaviour behaviour) {
        this.permissions = permissions;
        this.behaviour = behaviour;
    }

    public List<MqttTopicPermission> getMqttTopicPermissions() {
        return this.permissions;
    }

    public AuthorizationBehaviour getDefaultBehaviour() {
        return this.behaviour;
    }
}
