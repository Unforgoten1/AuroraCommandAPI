package dev.aurora.template;

import dev.aurora.Command.AuroraCommand;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Fluent builder for creating custom command templates.
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * CommandTemplate customTemplate = TemplateBuilder.create("moderator")
 *     .description("For moderator commands")
 *     .requirePermission("server.moderator")
 *     .withCooldown(2000)
 *     .withLogging()
 *     .build();
 * }</pre>
 */
public class TemplateBuilder {
    private String name;
    private String description = "";
    private final List<Consumer<AuroraCommand>> configurators;

    private TemplateBuilder(String name) {
        this.name = name;
        this.configurators = new ArrayList<>();
    }

    /**
     * Create a new template builder.
     *
     * @param name Template name
     * @return New builder
     */
    public static TemplateBuilder create(String name) {
        return new TemplateBuilder(name);
    }

    /**
     * Set the template description.
     *
     * @param description Template description
     * @return This builder
     */
    public TemplateBuilder description(String description) {
        this.description = description;
        return this;
    }

    /**
     * Add a permission requirement.
     *
     * @param permission The required permission
     * @return This builder
     */
    public TemplateBuilder requirePermission(String permission) {
        configurators.add(cmd -> cmd.setPermission(permission));
        return this;
    }

    /**
     * Require OP status.
     *
     * @return This builder
     */
    public TemplateBuilder requireOp() {
        configurators.add(AuroraCommand::requireOp);
        return this;
    }

    /**
     * Require player sender.
     *
     * @return This builder
     */
    public TemplateBuilder requirePlayer() {
        configurators.add(AuroraCommand::requirePlayer);
        return this;
    }

    /**
     * Require console sender.
     *
     * @return This builder
     */
    public TemplateBuilder requireConsole() {
        configurators.add(AuroraCommand::requireConsole);
        return this;
    }

    /**
     * Add a cooldown.
     *
     * @param milliseconds Cooldown in milliseconds
     * @return This builder
     */
    public TemplateBuilder withCooldown(long milliseconds) {
        configurators.add(cmd -> cmd.setCooldown(milliseconds));
        return this;
    }

    /**
     * Require confirmation.
     *
     * @return This builder
     */
    public TemplateBuilder requireConfirmation() {
        configurators.add(AuroraCommand::requireConfirmation);
        return this;
    }

    /**
     * Require confirmation with custom timeout and message.
     *
     * @param timeoutSeconds Confirmation timeout in seconds
     * @param message Confirmation message
     * @return This builder
     */
    public TemplateBuilder requireConfirmation(long timeoutSeconds, String message) {
        configurators.add(cmd -> cmd.requireConfirmation(timeoutSeconds, message));
        return this;
    }

    /**
     * Add logging middleware.
     *
     * @return This builder
     */
    public TemplateBuilder withLogging() {
        configurators.add(cmd -> cmd.addMiddleware(new dev.aurora.middleware.builtin.LoggingMiddleware()));
        return this;
    }

    /**
     * Execute async.
     *
     * @return This builder
     */
    public TemplateBuilder async() {
        configurators.add(cmd -> cmd.setAsync(true));
        return this;
    }

    /**
     * Add a custom configurator.
     *
     * @param configurator Custom configuration function
     * @return This builder
     */
    public TemplateBuilder configure(Consumer<AuroraCommand> configurator) {
        configurators.add(configurator);
        return this;
    }

    /**
     * Build the template.
     *
     * @return The configured template
     */
    public CommandTemplate build() {
        return new CommandTemplate(name, description, cmd -> {
            for (Consumer<AuroraCommand> configurator : configurators) {
                configurator.accept(cmd);
            }
        });
    }
}
