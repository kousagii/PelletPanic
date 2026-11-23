package pelletpanic;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Serializes file writes outside the Swing event thread. */
final class GamePersistence {
    private static final ExecutorService WORKER = Executors.newSingleThreadExecutor(action -> {
        Thread thread = new Thread(action, "pellet-panic-saves");
        thread.setDaemon(true);
        return thread;
    });

    private GamePersistence() {
    }

    static <T> CompletableFuture<T> submit(Callable<T> operation) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return operation.call();
            } catch (Exception exception) {
                throw new CompletionException(exception);
            }
        }, WORKER);
    }
}
