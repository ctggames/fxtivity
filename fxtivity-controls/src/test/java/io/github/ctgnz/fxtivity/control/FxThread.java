package io.github.ctgnz.fxtivity.control;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import javafx.application.Platform;

/**
 * Runs test code on the JavaFX application thread, where controls are built and used, starting the toolkit the first time.
 * <p>
 * Starting the toolkit needs a display: on a display-less CI runner the build runs under Xvfb, without which startup hangs rather than failing.
 */
final class FxThread {
    private static boolean started;

    private FxThread() {
    }

    /** Runs {@code action} on the application thread, and rethrows whatever it throws. */
    static void run(ThrowingRunnable action) {
        call(() -> {
            action.run();
            return null;
        });
    }

    /** Calls {@code action} on the application thread, and returns its result or rethrows whatever it throws. */
    static <T> T call(Callable<T> action) {
        start();
        CompletableFuture<T> result = new CompletableFuture<>();
        Platform.runLater(() -> {
            try {
                result.complete(action.call());
            } catch (Throwable t) {
                result.completeExceptionally(t);
            }
        });
        try {
            return result.get(10, TimeUnit.SECONDS);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof RuntimeException runtime) {
                throw runtime;
            }
            if (e.getCause() instanceof Error error) {
                throw error;
            }
            throw new IllegalStateException(e.getCause());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static synchronized void start() {
        if (!started) {
            CompletableFuture<Void> ready = new CompletableFuture<>();
            Platform.startup(() -> ready.complete(null));
            ready.join();
            Platform.setImplicitExit(false);
            started = true;
        }
    }

    /** Test code that may throw. */
    @FunctionalInterface
    interface ThrowingRunnable {
        void run() throws Exception;
    }

}
