package bh1;

import com.hivemq.spi.annotations.NotNull;
import jetbrains.exodus.env.Environment;
import jetbrains.exodus.env.Store;

import java.util.concurrent.locks.ReadWriteLock;

public class LockBucket extends Bucket {
    @NotNull
    private final ReadWriteLock lock;

    public LockBucket(@NotNull Environment environment,
                      @NotNull Store store,
                      @NotNull ReadWriteLock lock) {
        super(environment, store);
        this.lock = lock;
    }

    @NotNull
    public ReadWriteLock getLock() {
        return lock;
    }
}
