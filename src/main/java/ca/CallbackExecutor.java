package ca;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;

public class CallbackExecutor {
    private final ListeningExecutorService executorService;

    public CallbackExecutor(ListeningExecutorService executorService) {
        this.executorService = executorService;
    }

    public <T> ListenableFuture<T> submit(CallableTask<T> task) {
        return this.executorService.submit(task);
    }

    public ListenableFuture<?> submit(RunnableTask task) {
        return this.executorService.submit(task);
    }
}
