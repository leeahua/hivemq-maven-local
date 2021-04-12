package cb;


import cg.PluginOnPublishReceivedCallbackHandler;
import ci.PluginOnAuthorizationPublishCallbackHandler;
import ck.PublishHandler;
import com.google.inject.Inject;
import d.CacheScoped;

@CacheScoped
public class EventBusSubscribers {
    private final PublishHandler publishHandler;
    private final PluginOnAuthorizationPublishCallbackHandler pluginOnAuthorizationPublishCallbackHandler;
    private final PluginOnPublishReceivedCallbackHandler pluginOnPublishReceivedCallbackHandler;

    @Inject
    public EventBusSubscribers(PublishHandler publishHandler,
                               PluginOnAuthorizationPublishCallbackHandler pluginOnAuthorizationPublishCallbackHandler,
                               PluginOnPublishReceivedCallbackHandler pluginOnPublishReceivedCallbackHandler) {
        this.publishHandler = publishHandler;
        this.pluginOnAuthorizationPublishCallbackHandler = pluginOnAuthorizationPublishCallbackHandler;
        this.pluginOnPublishReceivedCallbackHandler = pluginOnPublishReceivedCallbackHandler;
    }

    public PublishHandler getPublishHandler() {
        return publishHandler;
    }

    public PluginOnAuthorizationPublishCallbackHandler getPluginOnAuthorizationPublishCallbackHandler() {
        return pluginOnAuthorizationPublishCallbackHandler;
    }

    public PluginOnPublishReceivedCallbackHandler getPluginOnPublishReceivedCallbackHandler() {
        return pluginOnPublishReceivedCallbackHandler;
    }
}
