package bg1;

import av.PersistenceConfig;
import com.google.common.base.Preconditions;
import com.hivemq.spi.annotations.NotNull;
import com.hivemq.spi.annotations.Nullable;
import jetbrains.exodus.ByteIterable;
import jetbrains.exodus.env.Cursor;
import jetbrains.exodus.env.EnvironmentConfig;
import jetbrains.exodus.util.LightOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

public class XodusUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(XodusUtils.class);

    public static ByteIterable toByteIterable(@NotNull String in) {
        Preconditions.checkNotNull(in, "String must not be null");
        byte[] outBytes = in.getBytes(StandardCharsets.UTF_8);
        LightOutputStream outputStream = new LightOutputStream(outBytes.length);
        outputStream.write(outBytes);
        return outputStream.asArrayByteIterable();
    }

    public static ByteIterable toByteIterable(@NotNull byte[] in) {
        Preconditions.checkNotNull(in, "bytes must not be null");
        LightOutputStream outputStream = new LightOutputStream();
        outputStream.write(in);
        return outputStream.asArrayByteIterable();
    }

    public static String toString(@NotNull ByteIterable in) {
        Preconditions.checkNotNull(in, "ByteIterable must not be null");
        return new String(in.getBytesUnsafe(), 0, in.getLength(), StandardCharsets.UTF_8);
    }

    public static byte[] toBytes(@NotNull ByteIterable in) {
        Preconditions.checkNotNull(in, "ByteIterable must not be null");
        byte[] inBytes = in.getBytesUnsafe();
        if (inBytes.length == in.getLength()) {
            return inBytes;
        }
        byte[] outBytes = new byte[in.getLength()];
        System.arraycopy(inBytes, 0, outBytes, 0, outBytes.length);
        return outBytes;
    }

    public static EnvironmentConfig buildConfig(@NotNull PersistenceConfig config, @NotNull String name) {
        Preconditions.checkNotNull(name, "Name for environment config must not be null");
        Preconditions.checkNotNull(config, "Persistence Config for %s must not be null", name);
        EnvironmentConfig environmentConfig = new EnvironmentConfig();
        int gcMinAge = config.getGcMinAge();
        environmentConfig.setGcFileMinAge(gcMinAge);
        LOGGER.trace("Setting GC file min age for persistence {} to {}", name, gcMinAge);
        int gcDeletionDelay = config.getGcDeletionDelay();
        environmentConfig.setGcFilesDeletionDelay(gcDeletionDelay);
        LOGGER.trace("Setting GC files deletion delay for persistence {} to {}ms", name, gcDeletionDelay);
        int gcFilesInterval = config.getGcFilesInterval();
        environmentConfig.setGcFilesInterval(gcFilesInterval);
        LOGGER.trace("Setting GC files interval for persistence {} to {}ms", name, gcFilesInterval);
        int gcRunPeriod = config.getGcRunPeriod();
        environmentConfig.setGcRunPeriod(gcRunPeriod);
        LOGGER.trace("Setting GC run period for persistence {} to {}ms", name, gcRunPeriod);
        if (config.getGcType() == PersistenceConfig.GCType.RENAME) {
            environmentConfig.setGcRenameFiles(true);
        }
        LOGGER.trace("Setting mode for persistence {} to {}", name, config.getGcType().name());
        int syncPeriod = config.getSyncPeriod();
        environmentConfig.setLogSyncPeriod(syncPeriod);
        LOGGER.trace("Setting sync period for persistence {} to {}ms", name, syncPeriod);
        boolean durableWrites = config.isDurableWrites();
        environmentConfig.setLogDurableWrite(durableWrites);
        LOGGER.trace("Setting durable writes for persistence {} to {}", name, durableWrites);
        boolean jmxEnabled = config.isJmxEnabled();
        environmentConfig.setManagementEnabled(jmxEnabled);
        LOGGER.trace("Setting JMX enabled for persistence {} to {}", name, jmxEnabled);
        return environmentConfig;
    }


    public static void close(@Nullable Cursor cursor) {
        close(cursor, null);
    }

    public static void close(@Nullable Cursor cursor, @Nullable Throwable cause) {
        if (cursor == null) {
            return;
        }
        if (cause == null) {
            cursor.close();
            return;
        }
        try {
            cursor.close();
        } catch (Throwable e) {
            cause.addSuppressed(e);
        }
    }
}
