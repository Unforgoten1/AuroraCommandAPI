package dev.aurora.metrics;

import dev.aurora.middleware.CommandMiddleware;
import dev.aurora.struct.CommandContext;
import org.bukkit.command.CommandSender;

/**
 * Middleware that automatically tracks command execution metrics.
 * Records execution time, success/failure, and other statistics.
 */
public class MetricsMiddleware implements CommandMiddleware {
    private final CommandMetrics metrics;
    private final ThreadLocal<Long> startTime;

    public MetricsMiddleware(CommandMetrics metrics) {
        this.metrics = metrics;
        this.startTime = new ThreadLocal<>();
    }

    @Override
    public boolean before(CommandSender sender, CommandContext context) {
        startTime.set(System.currentTimeMillis());
        return true;
    }

    @Override
    public void after(CommandSender sender, CommandContext context, boolean success) {
        Long start = startTime.get();
        if (start != null) {
            long executionTime = System.currentTimeMillis() - start;
            String commandName = context.getCommandName();
            if (commandName != null) {
                metrics.recordExecution(commandName, executionTime, success);
            }
            startTime.remove();
        }
    }

    @Override
    public int getPriority() {
        return 1; // Execute early to capture accurate timing
    }
}
