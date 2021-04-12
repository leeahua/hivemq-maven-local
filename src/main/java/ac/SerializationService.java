package ac;

import ad.SerializationException;
import com.hivemq.spi.annotations.NotNull;

import java.io.InputStream;
import java.io.OutputStream;

public interface SerializationService {
    <T> T deserialize(@NotNull InputStream inputStream, @NotNull Class<T> type) throws SerializationException;

    void serialize(@NotNull Object object, @NotNull OutputStream outputStream);

    <T> T deserialize(@NotNull byte[] inputBytes, @NotNull Class<T> type) throws SerializationException;

    Object deserialize(@NotNull byte[] inputBytes) throws SerializationException;

    byte[] serializeObject(@NotNull Object object);

    <T> byte[] serialize(@NotNull T object);

    byte[] ok();

    byte[] ok(boolean value);
}
