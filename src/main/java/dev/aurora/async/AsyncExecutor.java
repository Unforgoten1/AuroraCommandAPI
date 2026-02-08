package dev.aurora.async;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Handles asynchronous command execution with safe callbacks to the main thread.
 *
 * WARNING: The Bukkit API is NOT thread-safe. Only use async execution for:
 * - Heavy computation (calculations, algorithms)
 * - File I/O operations
 * - Database queries
 * - HTTP requests
 *
 * Do NOT interact with the Bukkit API (players, worlds, blocks, etc.) from async threads.
 * Use thenSync() to execute Bukkit API calls on the main thread.
 */
public class AsyncExecutor {
    private final JavaPlugin plugin;

    public AsyncExecutor(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Executes a task asynchronously.
     *
     * @param task The task to execute
     * @return A BukkitTask representing the async operation
     */
    public BukkitTask runAsync(Runnable task) {
        return Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
    }

    /**
     * Executes a task asynchronously and returns a CompletableFuture.
     *
     * @param supplier The task that produces a result
     * @param <T>      The result type
     * @return A CompletableFuture that will complete with the result
     */
    public <T> CompletableFuture<T> supplyAsync(Supplier<T> supplier) {
        CompletableFuture<T> future = new CompletableFuture<>();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                T result = supplier.get();
                future.complete(result);
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });

        return future;
    }

    /**
     * Executes a task asynchronously, then runs a callback on the main thread.
     * This is the safe way to interact with the Bukkit API after async work.
     *
     * @param asyncTask The task to run asynchronously
     * @param syncCallback The callback to run on the main thread
     * @param <T> The result type from async task
     */
    public <T> void runAsyncThenSync(Supplier<T> asyncTask, Consumer<T> syncCallback) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                T result = asyncTask.get();

                // Run callback on main thread
                Bukkit.getScheduler().runTask(plugin, () -> {
                    try {
                        syncCallback.accept(result);
                    } catch (Exception e) {
                        plugin.getLogger().severe("Error in sync callback: " + e.getMessage());
                        e.printStackTrace();
                    }
                });
            } catch (Exception e) {
                plugin.getLogger().severe("Error in async task: " + e.getMessage());
                e.printStackTrace();

                // Still run callback on main thread with null
                Bukkit.getScheduler().runTask(plugin, () -> syncCallback.accept(null));
            }
        });
    }

    /**
     * Executes a task on the main thread.
     * Use this for callbacks that need to interact with Bukkit API.
     *
     * @param task The task to execute on the main thread
     * @return A BukkitTask representing the operation
     */
    public BukkitTask runSync(Runnable task) {
        return Bukkit.getScheduler().runTask(plugin, task);
    }

    /**
     * Executes a task on the main thread after a delay.
     *
     * @param task The task to execute
     * @param delayTicks The delay in ticks (20 ticks = 1 second)
     * @return A BukkitTask representing the delayed operation
     */
    public BukkitTask runSyncLater(Runnable task, long delayTicks) {
        return Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks);
    }

    /**
     * Checks if the current thread is the main server thread.
     *
     * @return True if on main thread, false if on async thread
     */
    public static boolean isMainThread() {
        return Bukkit.isPrimaryThread();
    }

    /**
     * Asserts that the current code is running on the main thread.
     * Throws an exception if called from an async thread.
     *
     * @throws IllegalStateException if not on main thread
     */
    public static void assertMainThread() {
        if (!isMainThread()) {
            throw new IllegalStateException("This operation must be performed on the main server thread!");
        }
    }

    /**
     * Asserts that the current code is NOT running on the main thread.
     * Throws an exception if called from the main thread.
     *
     * @throws IllegalStateException if on main thread
     */
    public static void assertAsyncThread() {
        if (isMainThread()) {
            throw new IllegalStateException("This operation must be performed asynchronously!");
        }
    }
}
