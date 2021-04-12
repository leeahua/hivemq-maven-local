package bl;

import bl1.LWTPersistence;
import bu.InternalPublish;
import bz.LWT;
import cb1.AttributeKeys;
import co.InternalPublishService;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.hivemq.spi.message.Connect;
import com.hivemq.spi.message.Disconnect;
import i.ClusterIdProducer;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import s.Cluster;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@ChannelHandler.Sharable
public class MqttLwtHandler extends ChannelDuplexHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(MqttLwtHandler.class);
    private final LWTPersistence lwtPersistence;
    private final InternalPublishService publishService;
    private final ClusterIdProducer clusterIdProducer;
    private final ListeningExecutorService clusterExecutor;

    @Inject
    MqttLwtHandler(LWTPersistence lwtPersistence,
                   InternalPublishService publishService,
                   ClusterIdProducer clusterIdProducer,
                   @Cluster ListeningExecutorService clusterExecutor) {
        this.lwtPersistence = lwtPersistence;
        this.publishService = publishService;
        this.clusterIdProducer = clusterIdProducer;
        this.clusterExecutor = clusterExecutor;
    }

    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof Disconnect) {
            ctx.pipeline().remove(this);
        }
        if (msg instanceof Connect) {
            Connect connect = (Connect) msg;
            if (!connect.isWill()) {
                LOGGER.trace("Removing LWT Handler from channel for client {}",
                        connect.getClientIdentifier());
                ctx.pipeline().remove(this);
            } else {
                saveLWTMessage(connect);
            }
        }
        super.channelRead(ctx, msg);
    }

    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        String clientId = ctx.channel().attr(AttributeKeys.MQTT_CLIENT_ID).get();
        if (clientId != null) {
            LWT lwt = this.lwtPersistence.remove(clientId);
            Boolean takenOver = ctx.channel().attr(AttributeKeys.MQTT_TAKEN_OVER).get();
            if (takenOver != null && !takenOver) {
                Boolean authenticated = ctx.channel().attr(AttributeKeys.AUTHENTICATED).get();
                if (lwt != null && authenticated != null && authenticated) {
                    LOGGER.trace("Sending out LWT message for topic {} and QoS {} with size {} for client {}",
                            lwt.getWillTopic(), lwt.getWillQoS().getQosNumber(), lwt.getWillPayload().length, clientId);
                    publishLWT(lwt);
                }
            }
        }
        super.channelInactive(ctx);
    }

    private void publishLWT(LWT lwt) {
        InternalPublish publish = new InternalPublish(this.clusterIdProducer.get());
        publish.setPayload(lwt.getWillPayload());
        publish.setTopic(lwt.getWillTopic());
        publish.setRetain(lwt.isRetain());
        publish.setQoS(lwt.getWillQoS());
        this.publishService.publish(publish, this.clusterExecutor);
    }

    private void saveLWTMessage(Connect connect) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Saving LWT message for topic {} and QoS {} with size {} for client {}",
                    connect.getWillTopic(),
                    connect.getWillQos().getQosNumber(),
                    connect.getWillMessage().length,
                    connect.getClientIdentifier());
        }
        LWT lwt = new LWT(connect.getWillTopic(), connect.getWillMessage(),
                connect.getWillQos(), connect.isWillRetain());
        this.lwtPersistence.persist(connect.getClientIdentifier(), lwt);
    }
}
