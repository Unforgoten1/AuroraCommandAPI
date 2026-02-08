package dev.aurora.condition;

import dev.aurora.struct.CommandContext;
import org.bukkit.command.CommandSender;

/**
 * Interface for command execution conditions.
 * Conditions determine whether a command can execute based on various factors.
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * command.addCondition(new ExecutionCondition() {
 *     public boolean test(CommandSender sender, CommandContext context) {
 *         // Only allow at night
 *         if (sender instanceof Player) {
 *             Player player = (Player) sender;
 *             long time = player.getWorld().getTime();
 *             return time >= 12000 && time < 24000;
 *         }
 *         return true;
 *     }
 *
 *     public String getFailureMessage() {
 *         return "§cThis command only works at night!";
 *     }
 * });
 * }</pre>
 */
public interface ExecutionCondition {

    /**
     * Test if the condition is met.
     *
     * @param sender The command sender
     * @param context The command context
     * @return True if condition is met, false otherwise
     */
    boolean test(CommandSender sender, CommandContext context);

    /**
     * Get the message to display when the condition fails.
     *
     * @return Failure message
     */
    default String getFailureMessage() {
        return "§cCommand execution conditions not met!";
    }

    /**
     * Get the condition name/description.
     *
     * @return Condition name
     */
    default String getName() {
        return "Custom Condition";
    }
}
