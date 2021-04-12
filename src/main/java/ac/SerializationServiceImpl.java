package ac;

import ab.ClusterResponseCode;
import ab.ClusterResult;
import ad.SerializationException;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.hivemq.spi.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import r.KryoProvider;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

public class SerializationServiceImpl implements SerializationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(SerializationService.class);
    private final ThreadLocal<Kryo> context;
    private final byte[] OK;
    private final byte[] OK_TRUE;
    private final byte[] OK_FALSE;

    @Inject
    public SerializationServiceImpl(KryoProvider kryoProvider) {
        this.context = new ThreadLocal() {
            protected Kryo initialValue() {
                return kryoProvider.get();
            }
        };
        this.OK = serialize(new ClusterResult(ClusterResponseCode.OK, null));
        this.OK_TRUE = serialize(new ClusterResult(ClusterResponseCode.OK, true));
        this.OK_FALSE = serialize(new ClusterResult(ClusterResponseCode.OK, false));
    }

    public <T> T deserialize(@NotNull InputStream inputStream, @NotNull Class<T> type) throws SerializationException {
        Preconditions.checkNotNull(inputStream, "Input stream must not be null");
        Preconditions.checkNotNull(type, "Type must not be null");
        Input input = new Input(inputStream);
        try {
            return this.context.get().readObject(input, type);
        } catch (Exception e) {
            throw new SerializationException("Exception while deserializing object from stream", e);
        } finally {
            input.close();
        }
    }

    public void serialize(@NotNull Object object, @NotNull OutputStream outputStream) {
        Preconditions.checkNotNull(object, "Object stream must not be null");
        Preconditions.checkNotNull(outputStream, "Output stream must not be null");
        Output output = new Output(outputStream);
        this.context.get().writeObject(output, object);
        output.close();
    }

    public <T> T deserialize(@NotNull byte[] inputBytes, @NotNull Class<T> type) throws SerializationException {
        Preconditions.checkNotNull(inputBytes, "Bytes must not be null");
        Preconditions.checkNotNull(type, "Type must not be null");
        Input input = new Input(new ByteArrayInputStream(inputBytes));
        try {
            return this.context.get().readObject(input, type);
        } catch (Exception e) {
            throw new SerializationException("Exception while deserializing object: " + Arrays.toString(inputBytes), e);
        } finally {
            input.close();
        }
    }

    public Object deserialize(@NotNull byte[] inputBytes) throws SerializationException {
        Preconditions.checkNotNull(inputBytes, "Bytes must not be null");
        Input input = new Input(new ByteArrayInputStream(inputBytes));
        try {
            return this.context.get().readClassAndObject(input);
        } catch (Exception e) {
            throw new SerializationException("Exception while deserializing object: " + Arrays.toString(inputBytes), e);
        } finally {
            input.close();
        }
    }

    public byte[] serializeObject(@NotNull Object object) {
        Preconditions.checkNotNull(object, "Object must not be null");
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Output output = new Output(outputStream);
        this.context.get().writeObject(output, object);
        output.flush();
        byte[] outputBytes = outputStream.toByteArray();
        output.close();
        return outputBytes;
    }

    public <T> byte[] serialize(@NotNull T object) {
        Preconditions.checkNotNull(object, "Object must not be null");
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Output output = new Output(outputStream);
        this.context.get().writeClassAndObject(output, object);
        output.flush();
        byte[] outputBytes = outputStream.toByteArray();
        output.close();
        return outputBytes;
    }

    public byte[] ok() {
        return this.OK;
    }

    public byte[] ok(boolean value) {
        if (value) {
            return this.OK_TRUE;
        }
        return this.OK_FALSE;
    }
}
