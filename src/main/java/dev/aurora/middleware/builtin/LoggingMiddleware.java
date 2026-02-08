package dev.aurora.middleware.builtin;

import dev.aurora.middleware.CommandMiddleware;
import dev.aurora.struct.CommandContext;
import org.bukkit.command.CommandSender;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Built-in middleware for logging command execution.
 * Logs command start, completion time, and errors.
 */
public class LoggingMiddleware implements CommandMiddleware {
    private final Logger logger;
    private final Level level;
    private final ThreadLocal<Long> startTime;

    /**
     * Create a logging middleware with INFO level.
     */
    public LoggingMiddleware() {
        this(Logger.getLogger("AuroraCommands"), Level.INFO);
    }

    /**
     * Create a logging middleware with a custom logger and level.
     *
     * @param logger The logger to use
     * @param level The log level
     */
    public LoggingMiddleware(Logger logger, Level level) {
        this.logger = logger;
        this.level = level;
        this.startTime = new ThreadLocal<>();
    }

    @Override
    public boolean before(CommandSender sender, CommandContext context) {
        startTime.set(System.currentTimeMillis());
        logger.log(level, String.format("[Command] %s executing /%s",
                sender.getName(),
                context.getCommandName()));
        return true;
    }

    @Override
    public void after(CommandSender sender, CommandContext context, boolean success) {
        Long start = startTime.get();
        if (start != null) {
            long duration = System.currentTimeMillis() - start;
            logger.log(level, String.format("[Command] %s completed /%s in %dms (success: %s)",
                    sender.getName(),
                    context.getCommandName(),
                    duration,
                    success));
            startTime.remove();
        }
    }

    @Override
    public boolean onError(CommandSender sender, CommandContext context, Throwable error) {
        logger.log(Level.SEVERE, String.format("[Command] %s error in /%s: %s",
                sender.getName(),
                context.getCommandName(),
                error.getMessage()), error);
        return false; // Don't handle, let other middleware see it
    }

    @Override
    public int getPriority() {
        return 1000; // Execute late so we capture all execution time
    }
}
