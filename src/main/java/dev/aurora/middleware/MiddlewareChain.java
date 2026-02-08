package dev.aurora.middleware;

import dev.aurora.struct.CommandContext;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;

/**
 * Manages a chain of middleware that intercepts command execution.
 * Middleware is executed in priority order (lowest priority number first).
 */
public class MiddlewareChain {
    private final List<CommandMiddleware> middlewareList;
    private final Logger logger;

    public MiddlewareChain() {
        this.middlewareList = new ArrayList<>();
        this.logger = Logger.getLogger("AuroraCommands");
    }

    /**
     * Add middleware to the chain.
     *
     * @param middleware The middleware to add
     */
    public void add(CommandMiddleware middleware) {
        middlewareList.add(middleware);
        // Sort by priority after adding
        middlewareList.sort(Comparator.comparingInt(CommandMiddleware::getPriority));
        logger.fine("Added middleware with priority " + middleware.getPriority() +
                    " to chain (total: " + middlewareList.size() + ")");
    }

    /**
     * Remove middleware from the chain.
     *
     * @param middleware The middleware to remove
     * @return True if removed, false if not found
     */
    public boolean remove(CommandMiddleware middleware) {
        return middlewareList.remove(middleware);
    }

    /**
     * Clear all middleware from the chain.
     */
    public void clear() {
        middlewareList.clear();
        logger.fine("Cleared middleware chain");
    }

    /**
     * Execute all "before" hooks in the chain.
     * If any middleware returns false, execution stops and false is returned.
     *
     * @param sender The command sender
     * @param context The command context
     * @return True if all middleware passed, false if any cancelled execution
     */
    public boolean executeBefore(CommandSender sender, CommandContext context) {
        for (CommandMiddleware middleware : middlewareList) {
            try {
                boolean shouldContinue = middleware.before(sender, context);
                if (!shouldContinue) {
                    logger.fine("Middleware cancelled execution at priority " +
                               middleware.getPriority());
                    return false;
                }
            } catch (Exception e) {
                logger.warning("Error in middleware.before() at priority " +
                              middleware.getPriority() + ": " + e.getMessage());
                // Continue with other middleware even if one fails
            }
        }
        return true;
    }

    /**
     * Execute all "after" hooks in the chain (in reverse priority order).
     *
     * @param sender The command sender
     * @param context The command context
     * @param success Whether the command succeeded
     */
    public void executeAfter(CommandSender sender, CommandContext context, boolean success) {
        // Execute in reverse order (highest priority first for cleanup)
        for (int i = middlewareList.size() - 1; i >= 0; i--) {
            CommandMiddleware middleware = middlewareList.get(i);
            try {
                middleware.after(sender, context, success);
            } catch (Exception e) {
                logger.warning("Error in middleware.after() at priority " +
                              middleware.getPriority() + ": " + e.getMessage());
            }
        }
    }

    /**
     * Execute all "onError" hooks in the chain.
     * If any middleware returns true (handled), stops and returns true.
     *
     * @param sender The command sender
     * @param context The command context
     * @param error The error that occurred
     * @return True if any middleware handled the error, false otherwise
     */
    public boolean executeOnError(CommandSender sender, CommandContext context, Throwable error) {
        for (CommandMiddleware middleware : middlewareList) {
            try {
                boolean handled = middleware.onError(sender, context, error);
                if (handled) {
                    logger.fine("Middleware handled error at priority " +
                               middleware.getPriority());
                    return true;
                }
            } catch (Exception e) {
                logger.warning("Error in middleware.onError() at priority " +
                              middleware.getPriority() + ": " + e.getMessage());
            }
        }
        return false;
    }

    /**
     * Check if the chain has any middleware.
     *
     * @return True if the chain has middleware, false otherwise
     */
    public boolean isEmpty() {
        return middlewareList.isEmpty();
    }

    /**
     * Get the number of middleware in the chain.
     *
     * @return The middleware count
     */
    public int size() {
        return middlewareList.size();
    }

    /**
     * Get all middleware in the chain.
     *
     * @return Unmodifiable list of middleware
     */
    public List<CommandMiddleware> getAll() {
        return new ArrayList<>(middlewareList);
    }
}
