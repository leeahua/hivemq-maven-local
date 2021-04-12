package ca;

import com.hivemq.spi.annotations.NotNull;

import java.util.concurrent.Callable;

public interface CallableTask<V> extends Callable<V> {
    @NotNull
    Class callbackType();
}
