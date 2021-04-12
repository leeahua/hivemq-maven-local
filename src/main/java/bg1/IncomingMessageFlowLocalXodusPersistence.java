package bg1;

import av.PersistenceConfigurationService;
import bc1.IncomingMessageFlowLocalPersistence;
import bd1.PersistenceFolders;
import bh1.Bucket;
import bh1.BucketUtils;
import com.hivemq.spi.annotations.NotNull;
import com.hivemq.spi.annotations.Nullable;
import com.hivemq.spi.annotations.ThreadSafe;
import com.hivemq.spi.exceptions.UnrecoverableException;
import com.hivemq.spi.message.MessageWithId;
import d.CacheScoped;
import jetbrains.exodus.ArrayByteIterable;
import jetbrains.exodus.ByteIterable;
import jetbrains.exodus.ExodusException;
import jetbrains.exodus.env.Cursor;
import jetbrains.exodus.env.Environment;
import jetbrains.exodus.env.EnvironmentConfig;
import jetbrains.exodus.env.Environments;
import jetbrains.exodus.env.Store;
import jetbrains.exodus.env.StoreConfig;
import jetbrains.exodus.util.LightOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;

@ThreadSafe
@CacheScoped
public class IncomingMessageFlowLocalXodusPersistence
        implements IncomingMessageFlowLocalPersistence {
    private static final Logger LOGGER = LoggerFactory.getLogger(IncomingMessageFlowLocalXodusPersistence.class);
    private static final IncomingMessageFlowSerializer SERIALIZER = new IncomingMessageFlowSerializer();
    private static final int BUCKET_COUNT = 8;
    public static final String ENVIRONMENT_NAME = "incoming_message_flow";
    public static final String CURRENT_VERSION = "030100";

    private final PersistenceFolders persistenceFolders;
    private final PersistenceConfigurationService persistenceConfigurationService;
    private ConcurrentHashMap<Integer, Bucket> buckets;

    @Inject
    IncomingMessageFlowLocalXodusPersistence(PersistenceFolders persistenceFolders,
                                             PersistenceConfigurationService persistenceConfigurationService) {
        this.persistenceFolders = persistenceFolders;
        this.persistenceConfigurationService = persistenceConfigurationService;
    }

    @PostConstruct
    void init() {
        try {
            EnvironmentConfig config = XodusUtils.buildConfig(this.persistenceConfigurationService.getMessageFlowIncomingConfig(),
                    ENVIRONMENT_NAME);
            this.buckets = new ConcurrentHashMap();
            for (int bucket = 0; bucket < BUCKET_COUNT; bucket++) {
                Environment environment = Environments.newInstance(
                        new File(this.persistenceFolders.create(ENVIRONMENT_NAME, "030100"), "incoming_message_flow_" + bucket),
                        config);
                Store store = environment.computeInTransaction(txn ->
                        environment.openStore("incoming_messages", StoreConfig.WITHOUT_DUPLICATES, txn)
                );
                this.buckets.put(bucket, new Bucket(environment, store));
            }
        } catch (ExodusException e) {
            LOGGER.error("An error occurred while opening the Xodus Incoming Message Local persistence. Is another HiveMQ instance running?");
            LOGGER.debug("Original Exception:", e);
            throw new UnrecoverableException(false);
        }
    }

    @Nullable
    public MessageWithId get(@NotNull String clientId, int messageId) {
        Bucket bucket = this.buckets.get(BucketUtils.bucket(clientId, BUCKET_COUNT));
        ByteIterable foundMessage = bucket.getEnvironment().computeInReadonlyTransaction(txn -> {
            ByteIterable key = buildKey(clientId, messageId);
            return bucket.getStore().get(txn, key);
        });
        if (foundMessage == null) {
            return null;
        }
        byte[] messageBytes = foundMessage.getBytesUnsafe();
        MessageWithId message = SERIALIZER.deserialize(messageBytes);
        LOGGER.trace("Returning message {} from persistence store for client {} and message id {}",
                message, clientId, messageId);
        return message;
    }

    @NotNull
    private ByteIterable buildKey(String clientId, int messageId) {
        byte[] clientIdBytes = clientId.getBytes(StandardCharsets.UTF_8);
        int clientIdLength = clientIdBytes.length;
        int messageIdLength = 2;
        LightOutputStream lightOutputStream = new LightOutputStream(clientIdLength + messageIdLength);
        lightOutputStream.write(clientIdBytes);
        lightOutputStream.writeUnsignedShort(messageId);
        return lightOutputStream.asArrayByteIterable();
    }

    public void addOrReplace(@NotNull String clientId, int messageId, @NotNull MessageWithId message) {
        Bucket bucket = this.buckets.get(BucketUtils.bucket(clientId, BUCKET_COUNT));
        bucket.getEnvironment().executeInTransaction(txn -> {
            LOGGER.trace("Adding or replacing message {} to persistence store for client {} and message id {}",
                    message.getClass(), clientId, messageId);
            bucket.getStore().put(txn,
                    buildKey(clientId, messageId),
                    new ArrayByteIterable(SERIALIZER.serialize(message)));
        });
    }

    public void remove(@NotNull String clientId, int messageId) {
        Bucket bucket = this.buckets.get(BucketUtils.bucket(clientId, BUCKET_COUNT));
        bucket.getEnvironment().executeInTransaction(txn -> {
            LOGGER.trace("Deleting message from persistence store for client {} and message id {}",
                    clientId, messageId);
            bucket.getStore().delete(txn, buildKey(clientId, messageId));
        });
    }

    public void removeAll(@NotNull String clientId) {
        LOGGER.trace("Deleting all messages from persistence store for client {}", clientId);
        Bucket bucket = this.buckets.get(BucketUtils.bucket(clientId, BUCKET_COUNT));
        bucket.getEnvironment().executeInTransaction(txn -> {
            byte[] clientIdBytes = clientId.getBytes(StandardCharsets.UTF_8);
            LightOutputStream lightOutputStream = new LightOutputStream(clientIdBytes.length);
            lightOutputStream.write(clientIdBytes);
            ArrayByteIterable searchKey = lightOutputStream.asArrayByteIterable();
            Cursor cursor = bucket.getStore().openCursor(txn);
            cursor.getSearchKeyRange(searchKey);
            if (cursor.count() <= 0) {
                return;
            }
            do {
                int length = cursor.getKey().getLength();
                if (length == clientIdBytes.length + 2) {
                    cursor.deleteCurrent();
                }
            } while (cursor.getNext());
        });
    }

    public void close() {
        this.buckets.values().stream()
                .filter(bucket -> bucket.getEnvironment().isOpen())
                .forEach(bucket -> bucket.getEnvironment().close());
    }
}
