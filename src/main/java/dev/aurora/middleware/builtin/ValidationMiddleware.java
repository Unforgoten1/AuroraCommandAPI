package dev.aurora.middleware.builtin;

import dev.aurora.middleware.CommandMiddleware;
import dev.aurora.struct.CommandContext;
import org.bukkit.command.CommandSender;

import java.util.function.BiPredicate;

/**
 * Built-in middleware for custom validation logic.
 * Allows running arbitrary checks before command execution.
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * command.addMiddleware(new ValidationMiddleware(
 *     (sender, ctx) -> !sender.getName().equals("Notch"),
 *     "Sorry Notch, you can't use this command!"
 * ));
 * }</pre>
 */
public class ValidationMiddleware implements CommandMiddleware {
    private final BiPredicate<CommandSender, CommandContext> validator;
    private final String failureMessage;
    private final int priority;

    /**
     * Create a validation middleware with default priority (50).
     *
     * @param validator The validation predicate (return true to allow execution)
     * @param failureMessage The message to send if validation fails
     */
    public ValidationMiddleware(BiPredicate<CommandSender, CommandContext> validator,
                               String failureMessage) {
        this(validator, failureMessage, 50);
    }

    /**
     * Create a validation middleware with custom priority.
     *
     * @param validator The validation predicate (return true to allow execution)
     * @param failureMessage The message to send if validation fails
     * @param priority The priority (lower executes first)
     */
    public ValidationMiddleware(BiPredicate<CommandSender, CommandContext> validator,
                               String failureMessage,
                               int priority) {
        this.validator = validator;
        this.failureMessage = failureMessage;
        this.priority = priority;
    }

    @Override
    public boolean before(CommandSender sender, CommandContext context) {
        boolean valid = validator.test(sender, context);
        if (!valid && failureMessage != null) {
            sender.sendMessage(failureMessage);
        }
        return valid;
    }

    @Override
    public int getPriority() {
        return priority;
    }

    /**
     * Create a simple validation middleware that checks a condition.
     *
     * @param condition The condition to check
     * @param failureMessage The message to send if condition is false
     * @return The middleware
     */
    public static ValidationMiddleware require(BiPredicate<CommandSender, CommandContext> condition,
                                              String failureMessage) {
        return new ValidationMiddleware(condition, failureMessage);
    }

    /**
     * Create a validation middleware that requires the sender to be a player.
     *
     * @param failureMessage The message to send if sender is not a player
     * @return The middleware
     */
    public static ValidationMiddleware requirePlayer(String failureMessage) {
        return new ValidationMiddleware(
            (sender, ctx) -> sender instanceof org.bukkit.entity.Player,
            failureMessage,
            10 // High priority
        );
    }

    /**
     * Create a validation middleware that requires the sender to be console.
     *
     * @param failureMessage The message to send if sender is not console
     * @return The middleware
     */
    public static ValidationMiddleware requireConsole(String failureMessage) {
        return new ValidationMiddleware(
            (sender, ctx) -> sender instanceof org.bukkit.command.ConsoleCommandSender,
            failureMessage,
            10 // High priority
        );
    }
}
