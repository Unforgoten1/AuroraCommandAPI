package dev.aurora.template;

import dev.aurora.Command.AuroraCommand;
import dev.aurora.middleware.builtin.LoggingMiddleware;

import java.util.function.Consumer;

/**
 * Represents a reusable command configuration template.
 * Templates allow applying common settings to multiple commands.
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * command.applyTemplate(TemplateRegistry.ADMIN);
 * // Automatically applies OP requirement, cooldown, logging, etc.
 * }</pre>
 */
public class CommandTemplate {
    private final String name;
    private final String description;
    private final Consumer<AuroraCommand> configurator;

    /**
     * Create a command template.
     *
     * @param name Template name
     * @param description Template description
     * @param configurator Function that configures a command
     */
    public CommandTemplate(String name, String description, Consumer<AuroraCommand> configurator) {
        this.name = name;
        this.description = description;
        this.configurator = configurator;
    }

    /**
     * Apply this template to a command.
     *
     * @param command The command to configure
     */
    public void apply(AuroraCommand command) {
        configurator.accept(command);
    }

    /**
     * Get the template name.
     *
     * @return Template name
     */
    public String getName() {
        return name;
    }

    /**
     * Get the template description.
     *
     * @return Template description
     */
    public String getDescription() {
        return description;
    }

    // Built-in Templates

    /**
     * Admin template: Requires OP, adds logging middleware, 1-second cooldown.
     */
    public static final CommandTemplate ADMIN = new CommandTemplate(
        "admin",
        "For admin-only commands with logging",
        cmd -> {
            cmd.requireOp();
            cmd.addMiddleware(new LoggingMiddleware());
            cmd.setCooldown(1000); // 1 second
        }
    );

    /**
     * Player template: Requires player sender, adds logging, 5-second cooldown.
     */
    public static final CommandTemplate PLAYER = new CommandTemplate(
        "player",
        "For player-only commands",
        cmd -> {
            cmd.requirePlayer();
            cmd.addMiddleware(new LoggingMiddleware());
            cmd.setCooldown(5000); // 5 seconds
        }
    );

    /**
     * Dangerous template: Requires OP, confirmation, logging, 30-second cooldown.
     */
    public static final CommandTemplate DANGEROUS = new CommandTemplate(
        "dangerous",
        "For destructive operations requiring confirmation",
        cmd -> {
            cmd.requireOp();
            cmd.requireConfirmation(30, "§c⚠ WARNING: This is a destructive operation! Type §6/" +
                                       cmd.getName() + " confirm§c to proceed.");
            cmd.addMiddleware(new LoggingMiddleware());
            cmd.setCooldown(30000); // 30 seconds
        }
    );

    /**
     * Economy template: Player-only, rate limited, logging.
     */
    public static final CommandTemplate ECONOMY = new CommandTemplate(
        "economy",
        "For economy-related commands with rate limiting",
        cmd -> {
            cmd.requirePlayer();
            cmd.addMiddleware(new LoggingMiddleware());
            cmd.setCooldown(3000); // 3 seconds
            // Note: Rate limiting would be configured separately per command
        }
    );

    /**
     * Utility template: No special requirements, basic logging.
     */
    public static final CommandTemplate UTILITY = new CommandTemplate(
        "utility",
        "For basic utility commands",
        cmd -> {
            cmd.addMiddleware(new LoggingMiddleware());
            cmd.setCooldown(1000); // 1 second
        }
    );

    /**
     * Console template: Console-only commands with logging.
     */
    public static final CommandTemplate CONSOLE = new CommandTemplate(
        "console",
        "For console-only commands",
        cmd -> {
            cmd.requireConsole();
            cmd.addMiddleware(new LoggingMiddleware());
        }
    );
}
