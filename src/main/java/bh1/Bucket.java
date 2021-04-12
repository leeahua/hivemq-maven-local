package bh1;

import com.hivemq.spi.annotations.NotNull;
import jetbrains.exodus.env.Environment;
import jetbrains.exodus.env.Store;

public class Bucket {
    @NotNull
    private final Environment environment;
    @NotNull
    private final Store store;

    public Bucket(@NotNull Environment environment, @NotNull Store store) {
        this.environment = environment;
        this.store = store;
    }

    @NotNull
    public Environment getEnvironment() {
        return environment;
    }

    @NotNull
    public Store getStore() {
        return store;
    }
}
