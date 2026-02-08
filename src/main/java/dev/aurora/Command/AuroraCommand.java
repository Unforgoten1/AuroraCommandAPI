package dev.aurora.Command;

import dev.aurora.exception.ArgumentParseException;
import dev.aurora.Manager.CommandManager;
import dev.aurora.struct.ArgumentType;
import dev.aurora.struct.ArgumentBuilder;
import dev.aurora.struct.CommandContext;
import dev.aurora.struct.Types.Strings.GreedyStringArgumentType;
import dev.aurora.flags.CommandFlags;
import dev.aurora.flags.FlagBuilder;
import dev.aurora.confirmation.ConfirmationManager;
import dev.aurora.completion.SmartTabCompleter;
import dev.aurora.middleware.CommandMiddleware;
import dev.aurora.middleware.MiddlewareChain;
import dev.aurora.suggestion.CommandSuggester;
import dev.aurora.ratelimit.RateLimitConfig;
import dev.aurora.ratelimit.RateLimiter;
import dev.aurora.template.CommandTemplate;
import dev.aurora.condition.ExecutionCondition;
import dev.aurora.condition.ConditionalBuilder;
import dev.aurora.abtest.ABTest;
import dev.aurora.abtest.Variant;
import dev.aurora.transaction.CommandTransaction;
import dev.aurora.transaction.TransactionManager;
import dev.aurora.gui.CommandGUI;
import dev.aurora.gui.GUIBuilder;
import dev.aurora.pipeline.PipelineContext;
import dev.aurora.nlp.NLPParser;
import dev.aurora.history.UndoableCommand;
import dev.aurora.history.CommandHistory;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * The core class for defining and executing commands in the AuroraCommand API.
 * Supports fluent configuration of commands, subcommands, arguments, permissions, cooldowns, and tab completion.
 * Allows arbitrary argument names for flexible command design.
 */
public class AuroraCommand {
    private final String name;
    private final List<String> aliases;
    private String permission;
    private List<String> anyPermissions;
    private List<String> allPermissions;
    private Predicate<CommandSender> dynamicPermission;
    private boolean inheritParentPermissions;
    private long cooldownMillis;
    private final List<ArgumentEntry> arguments;
    private BiConsumer<CommandSender, CommandContext> executor;
    private Class<? extends CommandSender> senderType;
    private final List<AuroraCommand> subCommands;
    private final CommandManager manager;
    private final Logger logger;
    private boolean enabled;
    private boolean async;
    private String description;
    private final Map<String, String> customMessages;
    private CommandFlags commandFlags;
    private boolean requiresConfirmation;
    private long confirmationTimeoutSeconds;
    private String confirmationMessage;
    private SmartTabCompleter smartCompleter;
    private MiddlewareChain middlewareChain;
    private RateLimitConfig rateLimitConfig;
    private List<ExecutionCondition> executionConditions;
    private ABTest abTest;
    private boolean useTransaction;
    private boolean hasGUI;
    private String guiTitle;
    private boolean supportsPipeline;
    private Function<dev.aurora.pipeline.PipelineContext, Object> pipelineOutputExtractor;
    private UndoableCommand undoableCommand;

    // Inner class to store argument configuration
    private static class ArgumentEntry {
        private final String name;
        private final ArgumentType<?> type;
        private final Object defaultValue;
        private final Predicate<Object> validator;
        private final String validationErrorMessage;
        private final Predicate<CommandContext> condition;
        private final Map<String, Object> requiredValues;
        private final String description;

        // Simple constructor for basic arguments
        ArgumentEntry(String name, ArgumentType<?> type) {
            this.name = name;
            this.type = type;
            this.defaultValue = null;
            this.validator = null;
            this.validationErrorMessage = null;
            this.condition = null;
            this.requiredValues = new HashMap<>();
            this.description = null;
        }

        // Constructor from ArgumentBuilder
        @SuppressWarnings("unchecked")
        ArgumentEntry(ArgumentBuilder<?> builder) {
            this.name = builder.getName();
            this.type = builder.getType();
            this.defaultValue = builder.getDefaultValue();
            this.validator = (Predicate<Object>) builder.getValidator();
            this.validationErrorMessage = builder.getValidationErrorMessage();
            this.condition = builder.getCondition();
            this.requiredValues = new HashMap<>(builder.getRequiredValues());
            this.description = builder.getDescription();
        }

        String getName() {
            return name;
        }

        ArgumentType<?> getType() {
            return type;
        }

        Object getDefaultValue() {
            return defaultValue;
        }

        boolean hasDefault() {
            return defaultValue != null;
        }

        Predicate<Object> getValidator() {
            return validator;
        }

        String getValidationErrorMessage() {
            return validationErrorMessage;
        }

        boolean hasValidator() {
            return validator != null;
        }

        Predicate<CommandContext> getCondition() {
            return condition;
        }

        Map<String, Object> getRequiredValues() {
            return requiredValues;
        }

        boolean hasCondition() {
            return condition != null || !requiredValues.isEmpty();
        }

        String getDescription() {
            return description;
        }

        // Evaluate all conditions for this argument
        boolean evaluateConditions(CommandContext context) {
            if (condition != null && !condition.test(context)) {
                return false;
            }

            for (Map.Entry<String, Object> entry : requiredValues.entrySet()) {
                Object actualValue = context.getArgument(entry.getKey());
                Object requiredValue = entry.getValue();
                if (actualValue == null || !actualValue.equals(requiredValue)) {
                    return false;
                }
            }

            return true;
        }
    }

    /**
     * Constructs a new AuroraCommand with the specified name and manager.
     *
     * @param name    The command name (e.g., "message").
     * @param manager The CommandManager instance managing this command.
     */
    public AuroraCommand(String name, CommandManager manager) {
        this.name = name;
        this.manager = manager;
        this.aliases = new ArrayList<>();
        this.arguments = new ArrayList<>();
        this.subCommands = new ArrayList<>();
        this.senderType = CommandSender.class;
        this.logger = Logger.getLogger("InfusedAddons");
        this.anyPermissions = new ArrayList<>();
        this.allPermissions = new ArrayList<>();
        this.inheritParentPermissions = true;
        this.enabled = true;
        this.async = false;
        this.customMessages = new HashMap<>();
        this.commandFlags = new CommandFlags();
        this.requiresConfirmation = false;
        this.confirmationTimeoutSeconds = 30;
        this.confirmationMessage = null;
        this.smartCompleter = new SmartTabCompleter(manager);
        this.middlewareChain = new MiddlewareChain();
        this.executionConditions = new ArrayList<>();
        this.hasGUI = false;
        this.guiTitle = "§6Command Menu";
        this.supportsPipeline = false;
        this.pipelineOutputExtractor = null;
    }

    /**
     * Adds an alias for the command.
     *
     * @param alias The alias to add.
     * @return This AuroraCommand for chaining.
     */
    public AuroraCommand addAlias(String alias) {
        aliases.add(alias.toLowerCase());
        logger.info("Added alias '" + alias + "' for command: " + name);
        return this;
    }

    /**
     * Adds multiple aliases for the command.
     *
     * @param aliases The aliases to add.
     * @return This AuroraCommand for chaining.
     */
    public AuroraCommand addAliases(String... aliases) {
        for (String alias : aliases) {
            addAlias(alias);
        }
        return this;
    }

    /**
     * Sets the required permission for the command.
     *
     * @param permission The permission node (e.g., "infusedpvp.message").
     * @return This AuroraCommand for chaining.
     */
    public AuroraCommand addPermission(String permission) {
        this.permission = permission;
        logger.info("Set permission '" + permission + "' for command: " + name);
        return this;
    }

    /**
     * Sets the required permission (alias for addPermission).
     *
     * @param permission The permission node
     * @return This AuroraCommand for chaining
     */
    public AuroraCommand setPermission(String permission) {
        return addPermission(permission);
    }

    /**
     * Requires that the sender has ANY of the specified permissions.
     *
     * @param permissions The permission nodes (sender needs at least one)
     * @return This AuroraCommand for chaining
     */
    public AuroraCommand requiresAnyPermission(String... permissions) {
        this.anyPermissions = new ArrayList<>();
        for (String perm : permissions) {
            this.anyPermissions.add(perm);
        }
        logger.info("Set ANY permissions for command: " + name + " - " + String.join(", ", permissions));
        return this;
    }

    /**
     * Requires that the sender has ALL of the specified permissions.
     *
     * @param permissions The permission nodes (sender needs all of them)
     * @return This AuroraCommand for chaining
     */
    public AuroraCommand requiresAllPermissions(String... permissions) {
        this.allPermissions = new ArrayList<>();
        for (String perm : permissions) {
            this.allPermissions.add(perm);
        }
        logger.info("Set ALL permissions for command: " + name + " - " + String.join(", ", permissions));
        return this;
    }

    /**
     * Adds a dynamic permission check using a predicate.
     * The predicate receives the CommandSender and returns true if they have permission.
     *
     * @param permissionCheck The permission predicate
     * @return This AuroraCommand for chaining
     */
    public AuroraCommand addDynamicPermission(Predicate<CommandSender> permissionCheck) {
        this.dynamicPermission = permissionCheck;
        logger.info("Set dynamic permission check for command: " + name);
        return this;
    }

    /**
     * Sets whether this command inherits parent command permissions.
     * Default is true (subcommands require parent permissions).
     *
     * @param inherit Whether to inherit parent permissions
     * @return This AuroraCommand for chaining
     */
    public AuroraCommand inheritPermissions(boolean inherit) {
        this.inheritParentPermissions = inherit;
        logger.info("Set permission inheritance to " + inherit + " for command: " + name);
        return this;
    }

    /**
     * Sets a description for this command (used in help generation).
     *
     * @param description The command description
     * @return This AuroraCommand for chaining
     */
    public AuroraCommand setDescription(String description) {
        this.description = description;
        return this;
    }

    /**
     * Sets whether this command is enabled.
     * Disabled commands will show an error message when executed.
     *
     * @param enabled Whether the command is enabled
     * @return This AuroraCommand for chaining
     */
    public AuroraCommand setEnabled(boolean enabled) {
        this.enabled = enabled;
        logger.info("Set enabled to " + enabled + " for command: " + name);
        return this;
    }

    /**
     * Sets whether this command executes asynchronously.
     * WARNING: Bukkit API is NOT thread-safe. Only use async for heavy computation,
     * file I/O, database, or HTTP requests. Do NOT interact with Bukkit API from async threads.
     *
     * @param async Whether to execute asynchronously
     * @return This AuroraCommand for chaining
     */
    public AuroraCommand executeAsync(boolean async) {
        this.async = async;
        logger.info("Set async execution to " + async + " for command: " + name);
        return this;
    }

    /**
     * Sets async execution (alias for executeAsync).
     *
     * @param async Whether to execute asynchronously
     * @return This AuroraCommand for chaining
     */
    public AuroraCommand setAsync(boolean async) {
        return executeAsync(async);
    }

    /**
     * Shortcut to require that the sender is a player.
     * Equivalent to .addExecution(Player.class, executor)
     *
     * @return This AuroraCommand for chaining
     */
    public AuroraCommand requirePlayer() {
        this.senderType = Player.class;
        logger.info("Command " + name + " now requires player sender");
        return this;
    }

    /**
     * Shortcut to require that the sender is console.
     * Equivalent to .addExecution(ConsoleCommandSender.class, executor)
     *
     * @return This AuroraCommand for chaining
     */
    public AuroraCommand requireConsole() {
        this.senderType = org.bukkit.command.ConsoleCommandSender.class;
        logger.info("Command " + name + " now requires console sender");
        return this;
    }

    /**
     * Shortcut to require that the sender is an operator.
     *
     * @return This AuroraCommand for chaining
     */
    public AuroraCommand requireOp() {
        this.dynamicPermission = CommandSender::isOp;
        logger.info("Command " + name + " now requires OP");
        return this;
    }

    /**
     * Sets a custom error message for a specific error type.
     * Valid keys: "permission", "cooldown", "usage", "wrongSender", "disabled"
     *
     * @param key     The error message key
     * @param message The custom message (supports color codes with §)
     * @return This AuroraCommand for chaining
     */
    public AuroraCommand withErrorMessage(String key, String message) {
        this.customMessages.put(key.toLowerCase(), message);
        logger.info("Set custom error message for " + name + ": " + key);
        return this;
    }

    /**
     * Sets a custom error message (alias for withErrorMessage).
     *
     * @param key     The error message key
     * @param message The custom message (supports color codes with §)
     * @return This AuroraCommand for chaining
     */
    public AuroraCommand setCustomMessage(String key, String message) {
        return withErrorMessage(key, message);
    }

    /**
     * Gets a custom error message or returns the default.
     *
     * @param key            The error message key
     * @param defaultMessage The default message if no custom message is set
     * @return The error message to display
     */
    private String getErrorMessage(String key, String defaultMessage) {
        return customMessages.getOrDefault(key.toLowerCase(), defaultMessage);
    }

    /**
     * Sets a cooldown for the command in seconds.
     *
     * @param seconds The cooldown duration.
     * @return This AuroraCommand for chaining.
     */
    public AuroraCommand addCooldown(long seconds) {
        this.cooldownMillis = seconds * 1000;
        logger.info("Set cooldown " + seconds + " seconds for command: " + name);
        return this;
    }

    /**
     * Sets a cooldown for the command in milliseconds.
     *
     * @param milliseconds The cooldown duration in milliseconds
     * @return This AuroraCommand for chaining
     */
    public AuroraCommand setCooldown(long milliseconds) {
        this.cooldownMillis = milliseconds;
        logger.info("Set cooldown " + milliseconds + " ms for command: " + name);
        return this;
    }

    /**
     * Require confirmation before executing this command.
     * Uses default 30-second timeout and default message.
     *
     * @return This AuroraCommand for chaining
     */
    public AuroraCommand requireConfirmation() {
        this.requiresConfirmation = true;
        logger.info("Command " + name + " now requires confirmation");
        return this;
    }

    /**
     * Require confirmation before executing this command.
     *
     * @param timeoutSeconds How long the confirmation is valid (seconds)
     * @param message The message to show when confirmation is required
     * @return This AuroraCommand for chaining
     */
    public AuroraCommand requireConfirmation(long timeoutSeconds, String message) {
        this.requiresConfirmation = true;
        this.confirmationTimeoutSeconds = timeoutSeconds;
        this.confirmationMessage = message;
        logger.info("Command " + name + " requires confirmation (timeout: " + timeoutSeconds + "s)");
        return this;
    }

    /**
     * Add middleware to intercept command execution.
     * Middleware can run before, after, or on error.
     *
     * @param middleware The middleware to add
     * @return This AuroraCommand for chaining
     */
    public AuroraCommand addMiddleware(CommandMiddleware middleware) {
        this.middlewareChain.add(middleware);
        logger.info("Command " + name + " added middleware with priority " + middleware.getPriority());
        return this;
    }

    /**
     * Get the middleware chain for this command.
     *
     * @return The middleware chain
     */
    public MiddlewareChain getMiddlewareChain() {
        return middlewareChain;
    }

    /**
     * Configure rate limiting for this command.
     *
     * @param config The rate limit configuration
     * @return This AuroraCommand for chaining
     */
    public AuroraCommand withRateLimit(RateLimitConfig config) {
        this.rateLimitConfig = config;
        logger.info("Command " + name + " configured with rate limiting");
        return this;
    }

    /**
     * Get the rate limit configuration for this command.
     *
     * @return The rate limit config, or null if not configured
     */
    public RateLimitConfig getRateLimitConfig() {
        return rateLimitConfig;
    }

    /**
     * Apply a command template to configure this command.
     *
     * @param template The template to apply
     * @return This AuroraCommand for chaining
     */
    public AuroraCommand applyTemplate(CommandTemplate template) {
        template.apply(this);
        logger.info("Applied template '" + template.getName() + "' to command: " + name);
        return this;
    }

    /**
     * Apply a template by name from the registry.
     *
     * @param templateName The template name
     * @return This AuroraCommand for chaining
     */
    public AuroraCommand applyTemplate(String templateName) {
        CommandTemplate template = manager.getTemplateRegistry().getTemplate(templateName);
        if (template != null) {
            return applyTemplate(template);
        } else {
            logger.warning("Template not found: " + templateName);
            return this;
        }
    }

    /**
     * Add an execution condition to this command.
     * Conditions determine whether the command can execute.
     *
     * @param condition The condition to add
     * @return This AuroraCommand for chaining
     */
    public AuroraCommand addCondition(ExecutionCondition condition) {
        this.executionConditions.add(condition);
        logger.info("Added condition '" + condition.getName() + "' to command: " + name);
        return this;
    }

    /**
     * Add a condition using a builder.
     * Alias for addCondition() for more readable code.
     *
     * @param condition The condition to add
     * @return This AuroraCommand for chaining
     */
    public AuroraCommand when(ExecutionCondition condition) {
        return addCondition(condition);
    }

    /**
     * Get all execution conditions.
     *
     * @return List of conditions
     */
    public List<ExecutionCondition> getExecutionConditions() {
        return new ArrayList<>(executionConditions);
    }

    /**
     * Enable A/B testing for this command.
     * Players will be randomly assigned to variants for testing different implementations.
     *
     * @param abTest The A/B test configuration
     * @return This AuroraCommand for chaining
     */
    public AuroraCommand withABTest(ABTest abTest) {
        this.abTest = abTest;
        manager.getABTestManager().registerTest(abTest);
        logger.info("Command " + name + " configured with A/B test: " + abTest.getTestName());
        return this;
    }

    /**
     * Get the A/B test for this command.
     *
     * @return The A/B test, or null if not configured
     */
    public ABTest getABTest() {
        return abTest;
    }

    /**
     * Enable transactions for this command.
     * All changes will be rolled back automatically if an error occurs.
     *
     * @return This AuroraCommand for chaining
     */
    public AuroraCommand withTransaction() {
        this.useTransaction = true;
        logger.info("Command " + name + " enabled with transactions");
        return this;
    }

    /**
     * Check if transactions are enabled for this command.
     *
     * @return True if transactions are enabled
     */
    public boolean isTransactional() {
        return useTransaction;
    }

    /**
     * Enable GUI interface for this command.
     * Players can use /command gui to open an inventory-based interface.
     *
     * @return This AuroraCommand for chaining
     */
    public AuroraCommand enableGUI() {
        this.hasGUI = true;
        logger.info("Command " + name + " enabled with GUI");
        return this;
    }

    /**
     * Enable GUI interface for this command with a custom title.
     * Players can use /command gui to open an inventory-based interface.
     *
     * @param title The GUI inventory title
     * @return This AuroraCommand for chaining
     */
    public AuroraCommand enableGUI(String title) {
        this.hasGUI = true;
        this.guiTitle = title;
        logger.info("Command " + name + " enabled with GUI (title: " + title + ")");
        return this;
    }

    /**
     * Check if GUI is enabled for this command.
     *
     * @return True if GUI is enabled
     */
    public boolean hasGUI() {
        return hasGUI;
    }

    /**
     * Get the GUI title for this command.
     *
     * @return The GUI title
     */
    public String getGUITitle() {
        return guiTitle;
    }

    /**
     * Enable pipeline support for this command.
     * Allows this command to be used in pipeline chains with the | operator.
     *
     * @return This AuroraCommand for chaining
     */
    public AuroraCommand enablePipeline() {
        this.supportsPipeline = true;
        logger.info("Command " + name + " enabled with pipeline support");
        return this;
    }

    /**
     * Enable pipeline support with an output extractor.
     * The extractor function defines what data this command outputs to the next stage.
     *
     * @param outputExtractor Function to extract output from the pipeline context
     * @return This AuroraCommand for chaining
     */
    public AuroraCommand enablePipeline(Function<PipelineContext, Object> outputExtractor) {
        this.supportsPipeline = true;
        this.pipelineOutputExtractor = outputExtractor;
        logger.info("Command " + name + " enabled with pipeline support and output extractor");
        return this;
    }

    /**
     * Check if this command supports pipelines.
     *
     * @return True if pipeline support is enabled
     */
    public boolean supportsPipelines() {
        return supportsPipeline;
    }

    /**
     * Get the pipeline output extractor for this command.
     *
     * @return The output extractor function, or null if not set
     */
    public Function<PipelineContext, Object> getPipelineOutputExtractor() {
        return pipelineOutputExtractor;
    }

    /**
     * Add a natural language pattern for this command.
     * Allows players to execute this command using natural language.
     *
     * @param pattern The natural language pattern (e.g., "give me {amount} {item}")
     * @return This AuroraCommand for chaining
     */
    public AuroraCommand addNLPPattern(String pattern) {
        NLPParser parser = manager.getNLPParser();
        if (parser != null) {
            // Generate command template from this command's name and arguments
            StringBuilder template = new StringBuilder(name);
            for (ArgumentEntry arg : arguments) {
                template.append(" {").append(arg.getName()).append("}");
            }
            parser.addPattern(pattern, template.toString());
            logger.info("Added NLP pattern for " + name + ": " + pattern);
        } else {
            logger.warning("NLP parser not available, cannot add pattern");
        }
        return this;
    }

    /**
     * Make this command undoable.
     * Allows players to use /undo to reverse the command's effects.
     *
     * @param undoableCommand The undoable command implementation
     * @return This AuroraCommand for chaining
     */
    public AuroraCommand makeUndoable(UndoableCommand undoableCommand) {
        this.undoableCommand = undoableCommand;
        logger.info("Command " + name + " is now undoable");
        return this;
    }

    /**
     * Check if this command is undoable.
     *
     * @return True if undoable
     */
    public boolean isUndoable() {
        return undoableCommand != null;
    }

    /**
     * Get the undoable command implementation.
     *
     * @return The undoable command, or null if not undoable
     */
    public UndoableCommand getUndoableCommand() {
        return undoableCommand;
    }

    /**
     * Adds an argument to the command with a user-defined name and type.
     *
     * @param name The name of the argument (e.g., "customName").
     * @param type The argument type (e.g., StringArgumentType).
     * @return This AuroraCommand for chaining.
     */
    public AuroraCommand addArgument(String name, ArgumentType<?> type) {
        // Check for duplicate argument names
        for (ArgumentEntry existing : arguments) {
            if (existing.getName().equals(name)) {
                throw new IllegalArgumentException("Duplicate argument name '" + name + "' in command '" + this.name + "'. Each argument must have a unique name.");
            }
        }
        logger.info("Adding argument: name=" + name + ", type=" + type.getName());
        arguments.add(new ArgumentEntry(name, type));
        return this;
    }

    /**
     * Adds an argument to the command using an ArgumentBuilder for advanced configuration.
     * Supports defaults, validators, conditional parsing, and dependencies.
     *
     * @param builder The ArgumentBuilder containing the argument configuration
     * @return This AuroraCommand for chaining
     */
    public AuroraCommand addArgument(ArgumentBuilder<?> builder) {
        // Check for duplicate argument names
        for (ArgumentEntry existing : arguments) {
            if (existing.getName().equals(builder.getName())) {
                throw new IllegalArgumentException("Duplicate argument name '" + builder.getName() + "' in command '" + this.name + "'. Each argument must have a unique name.");
            }
        }
        logger.info("Adding argument with builder: name=" + builder.getName() + ", type=" + builder.getType().getName() +
                (builder.hasDefault() ? " (has default)" : "") +
                (builder.hasValidator() ? " (has validator)" : "") +
                (builder.hasCondition() ? " (has condition)" : ""));
        arguments.add(new ArgumentEntry(builder));
        return this;
    }

    /**
     * Adds a command flag/option.
     * Flags can be used with short form (-f) or long form (--flag) syntax.
     *
     * @param builder The FlagBuilder containing the flag configuration
     * @return This AuroraCommand for chaining
     */
    public AuroraCommand addFlag(FlagBuilder builder) {
        commandFlags.addFlag(builder.build());
        logger.info("Added flag to command: " + name + " - " + builder.build());
        return this;
    }

    /**
     * Sets the execution logic for the command.
     *
     * @param executor The execution logic.
     * @return This AuroraCommand for chaining.
     */
    public AuroraCommand addExecution(BiConsumer<CommandSender, CommandContext> executor) {
        this.executor = executor;
        logger.info("Set execution for command: " + name);
        return this;
    }

    /**
     * Sets the execution logic for the command, restricted to a specific sender type.
     *
     * @param senderType The type of sender (e.g., Player.class).
     * @param executor   The execution logic.
     * @return This AuroraCommand for chaining.
     */
    public AuroraCommand addExecution(Class<? extends CommandSender> senderType, BiConsumer<CommandSender, CommandContext> executor) {
        this.senderType = senderType;
        this.executor = executor;
        logger.info("Set execution for command: " + name + ", senderType: " + senderType.getSimpleName());
        return this;
    }

    /**
     * Adds a subcommand to this command.
     *
     * @param subCommand The subcommand to add.
     * @return This AuroraCommand for chaining.
     */
    public AuroraCommand addSubCommand(AuroraCommand subCommand) {
        subCommands.add(subCommand);
        logger.info("Added subcommand '" + subCommand.getName() + "' to command: " + name);
        return this;
    }

    /**
     * Registers the command with the CommandManager.
     */
    public void register() {
        manager.registerCommand(this);
        logger.info("Registered command: " + name);
    }

    /**
     * Executes the command or its subcommands.
     *
     * @param sender The sender executing the command.
     * @param args   The command arguments.
     * @throws ArgumentParseException If argument parsing fails.
     */
    public void execute(CommandSender sender, String[] args) throws ArgumentParseException {
        logger.info("Executing command: " + name + " for sender: " + sender.getName() + ", args: " + (args != null ? String.join(", ", args) : "null"));

        // Check if command is enabled
        if (!enabled) {
            sender.sendMessage(getErrorMessage("disabled", "§cThis command is currently disabled!"));
            logger.info("Command " + name + " is disabled");
            return;
        }

        if (!senderType.isInstance(sender)) {
            sender.sendMessage(getErrorMessage("wrongSender", "§cThis command is only for " + senderType.getSimpleName() + "!"));
            return;
        }

        // Check execution conditions
        if (!executionConditions.isEmpty()) {
            CommandContext tempContext = new CommandContext();
            tempContext.setCommandName(this.name);
            tempContext.addArgument("sender", sender);

            for (ExecutionCondition condition : executionConditions) {
                if (!condition.test(sender, tempContext)) {
                    sender.sendMessage(condition.getFailureMessage());
                    logger.info("Command " + name + " blocked by condition: " + condition.getName());
                    return;
                }
            }
        }

        // Check rate limits (if configured)
        if (rateLimitConfig != null) {
            RateLimiter rateLimiter = manager.getRateLimiter();
            if (!rateLimiter.tryAcquire(name, sender, rateLimitConfig)) {
                long waitTime = rateLimiter.getTimeUntilNextRequest(name, sender, rateLimitConfig);
                String message = rateLimitConfig.getRateLimitMessage();
                if (waitTime > 0) {
                    message += " §7(Wait " + waitTime + " seconds)";
                }
                sender.sendMessage(message);
                logger.info("Rate limit exceeded for command " + name + " by " + sender.getName());
                return;
            }
        }

        // Check for subcommands
        if (args != null && args.length > 0) {
            for (AuroraCommand subCommand : subCommands) {
                String[] finalArgs = args;
                if (subCommand.getName().equalsIgnoreCase(args[0]) || subCommand.getAliases().stream().anyMatch(alias -> alias.equalsIgnoreCase(finalArgs[0]))) {
                    if (!subCommand.hasPermission(sender)) {
                        sender.sendMessage("§cYou don't have permission!");
                        return;
                    }
                    if (subCommand.isOnCooldown(sender)) {
                        long remaining = subCommand.getCooldownRemaining(sender);
                        sender.sendMessage("§cSubcommand on cooldown! Wait " + (remaining / 1000) + " seconds.");
                        return;
                    }
                    subCommand.execute(sender, Arrays.copyOfRange(args, 1, args.length));
                    subCommand.applyCooldown(sender);
                    return;
                }
            }

            // If we have subcommands but didn't find a match, suggest similar ones
            if (!subCommands.isEmpty() && args[0] != null && !args[0].isEmpty()) {
                List<String> availableSubCommands = new ArrayList<>();
                for (AuroraCommand subCommand : subCommands) {
                    if (subCommand.hasPermission(sender)) {
                        availableSubCommands.add(subCommand.getName());
                        availableSubCommands.addAll(subCommand.getAliases());
                    }
                }

                if (!availableSubCommands.isEmpty()) {
                    String suggestion = CommandSuggester.createSuggestionMessage(args[0], availableSubCommands);
                    sender.sendMessage(suggestion);
                    logger.info("Unknown subcommand '" + args[0] + "' for " + name + ", sent suggestion");
                    return;
                }
            }
        }

        // Check for GUI subcommand
        if (hasGUI && args != null && args.length > 0 && args[0].equalsIgnoreCase("gui")) {
            if (sender instanceof Player) {
                CommandGUI commandGUI = manager.getCommandGUI();
                if (commandGUI != null) {
                    commandGUI.openGUI((Player) sender, this, guiTitle);
                    logger.info("Opened GUI for command " + name + " for player " + sender.getName());
                } else {
                    sender.sendMessage("§cGUI system not initialized!");
                    logger.warning("CommandGUI is null in CommandManager");
                }
            } else {
                sender.sendMessage("§cGUI is only available for players!");
            }
            return;
        }

        // Parse flags first (if any are defined)
        CommandFlags.ParsedFlags parsedFlags = null;
        if (commandFlags.hasFlags()) {
            try {
                parsedFlags = commandFlags.parse(sender, args);
                args = parsedFlags.remainingArgs(); // Use remaining args for arguments
                logger.info("Parsed flags for command: " + name + ", remaining args: " + Arrays.toString(args));
            } catch (ArgumentParseException e) {
                sender.sendMessage("§c" + e.getMessage());
                return;
            }
        }

        // Parse arguments
        CommandContext context = new CommandContext();
        context.setCommandName(this.name); // Set command name for middleware
        int argsIndex = 0; // Track position in args array

        for (int i = 0; i < arguments.size(); i++) {
            ArgumentEntry entry = arguments.get(i);
            String argName = entry.getName();
            ArgumentType<?> type = entry.getType();

            // Check if this argument should be parsed based on conditions
            if (entry.hasCondition() && !entry.evaluateConditions(context)) {
                logger.info("Skipping argument " + argName + " - condition not met");
                // Use default value if available
                if (entry.hasDefault()) {
                    context.addArgument(argName, entry.getDefaultValue());
                    logger.info("Using default value for " + argName + ": " + entry.getDefaultValue());
                }
                continue; // Skip this argument, don't consume input
            }

            String input;

            // Check if input is available or if we have a default
            if (argsIndex >= (args != null ? args.length : 0)) {
                if (entry.hasDefault()) {
                    context.addArgument(argName, entry.getDefaultValue());
                    logger.info("Using default value for " + argName + ": " + entry.getDefaultValue());
                    continue; // Move to next argument
                } else {
                    // No input and no default - show usage
                    sender.sendMessage("§cUsage: /" + name + " " + getUsage());
                    logger.warning("Insufficient arguments for " + name + " at argument " + argName);
                    return;
                }
            }

            // Handle GreedyStringArgumentType - consume all remaining arguments
            if (type instanceof GreedyStringArgumentType) {
                if (argsIndex < args.length) {
                    // Join all remaining arguments with spaces
                    StringBuilder greedy = new StringBuilder();
                    for (int j = argsIndex; j < args.length; j++) {
                        if (j > argsIndex) {
                            greedy.append(" ");
                        }
                        greedy.append(args[j]);
                    }
                    input = greedy.toString();
                    argsIndex = args.length; // Consumed all remaining args
                } else {
                    input = null;
                }
            } else {
                input = args[argsIndex];
                argsIndex++; // Consume one argument
            }

            try {
                logger.info("Parsing argument " + argName + " (type: " + type.getName() + ") with input: " + (input != null ? input : "null"));
                Object value = type.parse(sender, input);

                if (value == null) {
                    logger.warning("Parsed value is null for argument " + argName);
                }

                // Validate if validator is present
                if (entry.hasValidator()) {
                    if (!entry.getValidator().test(value)) {
                        String errorMsg = entry.getValidationErrorMessage() != null
                                ? entry.getValidationErrorMessage()
                                : "Invalid value for argument " + argName;
                        throw new ArgumentParseException(errorMsg, argName, input);
                    }
                    logger.info("Validation passed for argument " + argName);
                }

                context.addArgument(argName, value);
                logger.info("Added argument " + argName + ": " + (value != null ? value.toString() : "null"));
            } catch (ArgumentParseException e) {
                logger.warning("Failed to parse argument " + argName + ": " + e.getMessage());
                // Use enhanced error message if available
                if (e.getDetailedMessage() != null && !e.getDetailedMessage().equals(e.getMessage())) {
                    sender.sendMessage("§c" + e.getDetailedMessage());
                    return;
                }
                throw e;
            }
        }

        // Inject context variables (auto-inject common values)
        context.addArgument("sender", sender);
        if (sender instanceof Player) {
            Player player = (Player) sender;
            context.addArgument("senderLocation", player.getLocation());
            context.addArgument("senderWorld", player.getWorld());
        }
        context.addArgument("server", manager.getPlugin().getServer());

        // Inject flags into context (if any were parsed)
        if (parsedFlags != null) {
            context.addArgument("flags", parsedFlags);
            for (String flagName : parsedFlags.getFlagNames()) {
                context.addArgument("flag_" + flagName, parsedFlags.get(flagName));
            }
            logger.info("Injected " + parsedFlags.getFlagNames().size() + " flags into context");
        }

        // Handle confirmation requirement
        if (requiresConfirmation && executor != null) {
            ConfirmationManager confManager = ConfirmationManager.getInstance(manager.getPlugin());

            // Check if this is a confirmation
            if (args != null && args.length == 1 && args[0].equalsIgnoreCase("confirm")) {
                if (confManager.confirm(sender)) {
                    logger.info("Confirmation executed for " + sender.getName());
                    return; // Confirmation executed
                } else {
                    sender.sendMessage("§cNo pending command to confirm!");
                    return;
                }
            }

            // Queue for confirmation
            if (!(sender instanceof Player)) {
                sender.sendMessage("§cOnly players can use commands that require confirmation!");
                return;
            }

            final CommandSender finalSender = sender;
            final CommandContext finalContext = context;
            String msg = confirmationMessage != null ? confirmationMessage
                : "§e⚠ This is a destructive operation! Type §6/" + name + " confirm§e to proceed.";

            confManager.requireConfirmation(
                (Player) sender,
                () -> {
                    // Execute the command after confirmation with middleware
                    // Execute "before" middleware
                    boolean shouldContinue = middlewareChain.executeBefore(finalSender, finalContext);
                    if (!shouldContinue) {
                        logger.info("Middleware cancelled confirmed command execution");
                        return;
                    }

                    boolean success = false;
                    try {
                        if (async) {
                            Bukkit.getScheduler().runTaskAsynchronously(manager.getPlugin(), () -> {
                                try {
                                    executor.accept(finalSender, finalContext);
                                    middlewareChain.executeAfter(finalSender, finalContext, true);
                                } catch (Exception e) {
                                    logger.severe("Error in async confirmed command: " + e.getMessage());
                                    e.printStackTrace();
                                    boolean handled = middlewareChain.executeOnError(finalSender, finalContext, e);
                                    if (!handled) {
                                        Bukkit.getScheduler().runTask(manager.getPlugin(), () ->
                                            finalSender.sendMessage("§cAn error occurred while executing this command.")
                                        );
                                    }
                                    middlewareChain.executeAfter(finalSender, finalContext, false);
                                }
                            });
                        } else {
                            executor.accept(finalSender, finalContext);
                            success = true;
                        }
                    } catch (Exception e) {
                        logger.severe("Error in confirmed command: " + e.getMessage());
                        e.printStackTrace();
                        boolean handled = middlewareChain.executeOnError(finalSender, finalContext, e);
                        if (!handled) {
                            finalSender.sendMessage("§cAn error occurred while executing this command.");
                        }
                    } finally {
                        if (!async) {
                            middlewareChain.executeAfter(finalSender, finalContext, success);
                        }
                    }
                },
                confirmationTimeoutSeconds,
                msg
            );
            sender.sendMessage(msg);
            sender.sendMessage("§7This confirmation expires in " + confirmationTimeoutSeconds + " seconds.");
            logger.info("Confirmation required for " + sender.getName() + " on command: " + name);
            return; // Don't execute yet
        }

        // Execute command
        if (executor != null) {
            logger.info("Executing command with context: " + context.toString());

            // Execute "before" middleware
            boolean shouldContinue = middlewareChain.executeBefore(sender, context);
            if (!shouldContinue) {
                logger.info("Middleware cancelled command execution");
                return;
            }

            // Select executor (A/B test variant or default)
            BiConsumer<CommandSender, CommandContext> selectedExecutor = executor;
            Variant selectedVariant = null;

            if (abTest != null) {
                selectedVariant = abTest.getVariant(sender);
                if (selectedVariant.getExecutor() != null) {
                    selectedExecutor = selectedVariant.getExecutor();
                    logger.info("Using A/B test variant: " + selectedVariant.getName() +
                              " for command: " + name);
                }
            }

            final Variant finalVariant = selectedVariant;

            // Execute asynchronously if configured
            if (async) {
                logger.info("Executing command " + name + " asynchronously");
                final CommandSender finalSender = sender;
                final CommandContext finalContext = context;
                final BiConsumer<CommandSender, CommandContext> finalExecutor = selectedExecutor;
                Bukkit.getScheduler().runTaskAsynchronously(manager.getPlugin(), () -> {
                    long startTime = System.currentTimeMillis();
                    try {
                        finalExecutor.accept(finalSender, finalContext);
                        if (finalVariant != null) {
                            finalVariant.recordExecution(System.currentTimeMillis() - startTime, true);
                        }
                        middlewareChain.executeAfter(finalSender, finalContext, true);
                    } catch (Exception e) {
                        if (finalVariant != null) {
                            finalVariant.recordExecution(System.currentTimeMillis() - startTime, false);
                        }
                        logger.severe("Error in async command execution for " + name + ": " + e.getMessage());
                        e.printStackTrace();
                        // Let middleware handle the error
                        boolean handled = middlewareChain.executeOnError(finalSender, finalContext, e);
                        if (!handled) {
                            // Send error message on main thread
                            Bukkit.getScheduler().runTask(manager.getPlugin(), () ->
                                    finalSender.sendMessage("§cAn error occurred while executing this command.")
                            );
                        }
                        middlewareChain.executeAfter(finalSender, finalContext, false);
                    }
                });
            } else {
                long startTime = System.currentTimeMillis();
                boolean success = false;
                try {
                    selectedExecutor.accept(sender, context);
                    success = true;
                    if (selectedVariant != null) {
                        selectedVariant.recordExecution(System.currentTimeMillis() - startTime, true);
                    }

                    // Record in history if undoable and sender is a player
                    if (isUndoable() && sender instanceof Player) {
                        Runnable undoAction = undoableCommand.createUndoAction(sender, context);
                        if (undoAction != null) {
                            manager.getCommandHistory().recordCommand((Player) sender, name, args, undoAction);
                            logger.info("Recorded undoable command in history: " + name);
                        }
                    }
                } catch (Exception e) {
                    if (selectedVariant != null) {
                        selectedVariant.recordExecution(System.currentTimeMillis() - startTime, false);
                    }
                    logger.severe("Error in command execution for " + name + ": " + e.getMessage());
                    e.printStackTrace();
                    // Let middleware handle the error
                    boolean handled = middlewareChain.executeOnError(sender, context, e);
                    if (!handled) {
                        sender.sendMessage("§cAn error occurred while executing this command.");
                    }
                } finally {
                    middlewareChain.executeAfter(sender, context, success);
                }
            }
        } else if (subCommands.size() > 0) {
            sender.sendMessage("§cAvailable subcommands: " + getSubCommandNames());
        } else {
            sender.sendMessage("§cNo execution defined for this command.");
        }
    }

    /**
     * Checks if the sender has the required permission.
     *
     * @param sender The sender to check.
     * @return True if the sender has permission or no permission is set, false otherwise.
     */
    public boolean hasPermission(CommandSender sender) {
        // Check single permission
        if (permission != null && !permission.isEmpty()) {
            if (!sender.hasPermission(permission)) {
                logger.info("Sender " + sender.getName() + " lacks permission: " + permission);
                return false;
            }
        }

        // Check ANY permissions (needs at least one)
        if (!anyPermissions.isEmpty()) {
            boolean hasAny = false;
            for (String perm : anyPermissions) {
                if (sender.hasPermission(perm)) {
                    hasAny = true;
                    break;
                }
            }
            if (!hasAny) {
                logger.info("Sender " + sender.getName() + " lacks ANY of: " + String.join(", ", anyPermissions));
                return false;
            }
        }

        // Check ALL permissions (needs every one)
        if (!allPermissions.isEmpty()) {
            for (String perm : allPermissions) {
                if (!sender.hasPermission(perm)) {
                    logger.info("Sender " + sender.getName() + " lacks permission: " + perm + " (requires ALL)");
                    return false;
                }
            }
        }

        // Check dynamic permission
        if (dynamicPermission != null) {
            if (!dynamicPermission.test(sender)) {
                logger.info("Sender " + sender.getName() + " failed dynamic permission check");
                return false;
            }
        }

        return true;
    }

    /**
     * Checks if the command is on cooldown for the sender.
     *
     * @param sender The sender to check.
     * @return True if the command is on cooldown, false otherwise.
     */
    public boolean isOnCooldown(CommandSender sender) {
        if (cooldownMillis <= 0 || !(sender instanceof Player)) {
            return false;
        }
        boolean onCooldown = manager.getCooldownManager().isOnCooldown((Player) sender, name, cooldownMillis);
        logger.info("Checking cooldown for " + name + " by " + sender.getName() + ": onCooldown=" + onCooldown);
        return onCooldown;
    }

    /**
     * Gets the remaining cooldown time for the sender.
     *
     * @param sender The sender to check.
     * @return The remaining cooldown time in milliseconds, or 0 if not on cooldown.
     */
    public long getCooldownRemaining(CommandSender sender) {
        if (cooldownMillis <= 0 || !(sender instanceof Player)) {
            return 0;
        }
        return manager.getCooldownManager().getCooldownRemaining((Player) sender, name, cooldownMillis);
    }

    /**
     * Applies a cooldown to the sender.
     *
     * @param sender The sender to apply the cooldown to.
     */
    public void applyCooldown(CommandSender sender) {
        if (cooldownMillis <= 0 || !(sender instanceof Player)) {
            return;
        }
        manager.getCooldownManager().applyCooldown((Player) sender, name);
        logger.info("Applied cooldown for " + name + " to " + sender.getName());
    }

    /**
     * Gets tab completion suggestions for the command.
     *
     * @param sender The sender requesting completions.
     * @param args   The current arguments.
     * @return A list of completion suggestions.
     */
    public List<String> getTabCompletions(CommandSender sender, String[] args) {
        logger.info("Generating tab completions for " + name + ", args: " + (args != null ? String.join(", ", args) : "null"));
        if (args == null || args.length == 0) {
            return new ArrayList<>();
        }

        // Check if completing a flag
        String lastArg = args[args.length - 1];
        if (lastArg.startsWith("-") && commandFlags.hasFlags()) {
            List<String> flagCompletions = commandFlags.getFlagCompletions(lastArg);
            logger.info("Returning flag completions: " + flagCompletions);
            return flagCompletions;
        }

        // Suggest subcommands for the first argument
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            for (AuroraCommand subCommand : subCommands) {
                if (subCommand.hasPermission(sender)) {
                    completions.add(subCommand.getName());
                    completions.addAll(subCommand.getAliases());
                }
            }
            for (ArgumentEntry entry : arguments) {
                completions.addAll(entry.getType().getCompletions(sender));
            }
            // Use smart fuzzy matching instead of simple prefix filtering
            return smartCompleter.fuzzyMatch(args[0], completions);
        }

        // Suggest argument completions
        if (args.length <= arguments.size()) {
            ArgumentType<?> type = arguments.get(args.length - 1).getType();
            List<String> completions = type.getCompletions(sender);
            // Use smart fuzzy matching instead of simple prefix filtering
            return smartCompleter.fuzzyMatch(args[args.length - 1], completions);
        }

        // Suggest subcommand completions
        for (AuroraCommand subCommand : subCommands) {
            if (subCommand.getName().equalsIgnoreCase(args[0]) || subCommand.getAliases().stream().anyMatch(alias -> alias.equalsIgnoreCase(args[0]))) {
                if (subCommand.hasPermission(sender)) {
                    return subCommand.getTabCompletions(sender, Arrays.copyOfRange(args, 1, args.length));
                }
            }
        }

        return new ArrayList<>();
    }

    /**
     * Gets the usage string for the command.
     *
     * @return The usage string.
     */
    public String getUsage() {
        StringBuilder usage = new StringBuilder();
        for (ArgumentEntry entry : arguments) {
            // Show optional arguments (with defaults) in brackets, required in angle brackets
            if (entry.hasDefault() || entry.hasCondition()) {
                usage.append("[").append(entry.getName()).append("] ");
            } else {
                usage.append("<").append(entry.getName()).append("> ");
            }
        }
        for (AuroraCommand subCommand : subCommands) {
            if (usage.length() > 0) {
                usage.append("|");
            }
            usage.append(subCommand.getName());
        }
        return usage.toString().trim();
    }

    /**
     * Gets the names of available subcommands.
     *
     * @return A string of subcommand names.
     */
    public String getSubCommandNames() {
        return subCommands.stream()
                .map(AuroraCommand::getName)
                .collect(Collectors.joining(", "));
    }

    // Getters
    public String getName() {
        return name;
    }

    public List<String> getAliases() {
        return new ArrayList<>(aliases);
    }

    public String getPermission() {
        return permission;
    }

    public String getDescription() {
        return description;
    }

    public List<AuroraCommand> getSubCommands() {
        return new ArrayList<>(subCommands);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isAsync() {
        return async;
    }
}