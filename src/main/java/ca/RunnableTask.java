package ca;

import com.hivemq.spi.annotations.NotNull;

public interface RunnableTask extends Runnable {
    @NotNull
    Class callbackType();
}
