package cb1;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.sun.nio.file.SensitivityWatchEventModifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class DirectoryMonitor implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(DirectoryMonitor.class);
    private final Executor executor;
    private final String watchPath;
    private boolean watching;
    final Listener listener;
    private CountDownLatch latch;

    public DirectoryMonitor(String watchPath, Listener listener) {
        this.watchPath = watchPath;
        this.listener = listener;
        ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("hivemq-directory-monitor-%d").build();
        this.executor = Executors.newSingleThreadExecutor(threadFactory);
    }

    public void start() {
        this.watching = true;
        this.latch = new CountDownLatch(1);
        this.executor.execute(this);
        try {
            if (this.latch.await(5L, TimeUnit.SECONDS)) {
                return;
            }
        } catch (InterruptedException e) {
        }
        LOGGER.debug("Not able to start file watcher in time, runtime license upgrade may not be available");
    }

    public void stop() {
        this.watching = false;
    }

    public void run() {
        WatchService watchService = null;
        try {
            Path path = Paths.get(this.watchPath, new String[0]);
            watchService = createWatchService(path);
            this.latch.countDown();
            while (this.watching) {
                triggerEvent(watchService);
            }
            return;
        } catch (IOException | InterruptedException e) {
            this.latch.countDown();
            this.listener.onError(this.watchPath, e);
        } finally {
            if (watchService != null) {
                try {
                    watchService.close();
                } catch (IOException e) {
                    LOGGER.debug("Not able to close file watcher", e);
                }
            }
        }
    }


    private WatchService createWatchService(Path path) throws IOException {
        WatchService watchService = path.getFileSystem().newWatchService();
        path.register(watchService,
                new WatchEvent.Kind[]{StandardWatchEventKinds.ENTRY_CREATE,
                        StandardWatchEventKinds.ENTRY_DELETE,
                        StandardWatchEventKinds.ENTRY_MODIFY},
                new WatchEvent.Modifier[]{SensitivityWatchEventModifier.HIGH});
        return watchService;
    }

    private void triggerEvent(WatchService watchService) throws InterruptedException, IOException {
        WatchKey watchKey = watchService.poll(5L, TimeUnit.SECONDS);
        if (watchKey == null || !watchKey.isValid()) {
            return;
        }
        List<WatchEvent<?>> events = watchKey.pollEvents();
        events.forEach(event->{
            if (event.kind() == StandardWatchEventKinds.ENTRY_CREATE) {
                this.listener.onCreated(this.watchPath, event.context().toString());
            }
            if (event.kind() == StandardWatchEventKinds.ENTRY_DELETE) {
                this.listener.onDeleted(this.watchPath, event.context().toString());
            }
            if (event.kind() == StandardWatchEventKinds.ENTRY_MODIFY) {
                this.listener.onModify(this.watchPath, event.context().toString());
            }
            if (event.kind() == StandardWatchEventKinds.OVERFLOW) {
            }
        });
        if (!watchKey.reset()) {
            throw new IOException("Directory is not accessible");
        }
    }

    public boolean isWatching() {
        return watching;
    }

    public Listener getListener() {
        return listener;
    }

    public interface Listener {
        void onModify(String watchPath, String target);

        void onDeleted(String watchPath, String target);

        void onCreated(String watchPath, String target);

        void onError(String watchPath, Throwable cause);
    }
}
