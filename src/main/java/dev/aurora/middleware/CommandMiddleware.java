package dev.aurora.middleware;

import dev.aurora.struct.CommandContext;
import org.bukkit.command.CommandSender;

/**
 * Interface for command middleware that can intercept command execution.
 * Middleware can execute before the command, after the command, or on error.
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * command.addMiddleware(new CommandMiddleware() {
 *     public boolean before(CommandSender sender, CommandContext context) {
 *         // Log command execution
 *         System.out.println("Executing: " + context.getCommandName());
 *         return true; // Continue execution
 *     }
 *
 *     public void after(CommandSender sender, CommandContext context, boolean success) {
 *         System.out.println("Completed: " + success);
 *     }
 * });
 * }</pre>
 */
public interface CommandMiddleware {

    /**
     * Executed before the command runs.
     *
     * @param sender The command sender
     * @param context The command context
     * @return True to continue execution, false to cancel
     */
    default boolean before(CommandSender sender, CommandContext context) {
        return true;
    }

    /**
     * Executed after the command runs.
     *
     * @param sender The command sender
     * @param context The command context
     * @param success Whether the command succeeded
     */
    default void after(CommandSender sender, CommandContext context, boolean success) {
        // Default: do nothing
    }

    /**
     * Executed when an error occurs during command execution.
     *
     * @param sender The command sender
     * @param context The command context
     * @param error The error that occurred
     * @return True if the error was handled, false to propagate
     */
    default boolean onError(CommandSender sender, CommandContext context, Throwable error) {
        return false; // Don't handle by default
    }

    /**
     * Get the priority of this middleware.
     * Lower numbers execute first.
     * Default priority is 100.
     *
     * @return The priority
     */
    default int getPriority() {
        return 100;
    }
}
