package dev.aurora.Manager;

import dev.aurora.Command.AuroraCommand;
import dev.aurora.exception.ArgumentParseException;
import dev.aurora.struct.ArgumentType;
import dev.aurora.struct.ArgumentTypeRegistry;
import dev.aurora.struct.CommandTabCompleter;
import dev.aurora.struct.Types.Boolean.BooleanArgumentType;
import dev.aurora.struct.Types.Double.DoubleArgumentType;
import dev.aurora.struct.Types.Entity.EntityArgumentType;
import dev.aurora.struct.Types.Float.FloatArgumentType;
import dev.aurora.struct.Types.Integers.IntegerArgumentType;
import dev.aurora.struct.Types.Item.ItemDataArgumentType;
import dev.aurora.struct.Types.Item.ItemStackArgumentType;
import dev.aurora.struct.Types.Location.LocationArgumentType;
import dev.aurora.struct.Types.Player.AdminPlayerArgumentType;
import dev.aurora.struct.Types.Player.OnlinePlayerArgumentType;
import dev.aurora.struct.Types.Strings.StringArgumentType;
import dev.aurora.struct.Types.Strings.GreedyStringArgumentType;
import dev.aurora.struct.Types.World.WorldArgumentType;
import dev.aurora.struct.Types.Enchantment.EnchantmentArgumentType;
import dev.aurora.struct.Types.Potion.PotionEffectTypeArgumentType;
import dev.aurora.struct.Types.Sound.SoundArgumentType;
import dev.aurora.struct.Types.Player.OfflinePlayerArgumentType;
import dev.aurora.struct.Types.Time.TimeArgumentType;
import dev.aurora.struct.Types.Numbers.RangedIntegerArgumentType;
import dev.aurora.struct.Types.Numbers.RangedDoubleArgumentType;
import dev.aurora.struct.Types.Enum.EnumArgumentType;
import dev.aurora.struct.Types.Color.ColorArgumentType;
import dev.aurora.struct.Types.Multi.MultiValueArgumentType;
import dev.aurora.util.CooldownManager;
import dev.aurora.confirmation.ConfirmationManager;
import dev.aurora.suggestion.CommandSuggester;
import dev.aurora.ratelimit.RateLimiter;
import dev.aurora.template.TemplateRegistry;
import dev.aurora.metrics.CommandMetrics;
import dev.aurora.metrics.MetricsMiddleware;
import dev.aurora.commands.builtin.StatsCommand;
import dev.aurora.abtest.ABTestManager;
import dev.aurora.transaction.TransactionManager;
import dev.aurora.script.ScriptEngine;
import dev.aurora.gui.CommandGUI;
import dev.aurora.config.CommandLoader;
import dev.aurora.pipeline.CommandPipeline;
import dev.aurora.pipeline.PipelineExecutor;
import dev.aurora.pipeline.PipelineStage;
import dev.aurora.nlp.NLPParser;
import dev.aurora.nlp.NLPChatListener;
import dev.aurora.history.CommandHistory;
import dev.aurora.commands.builtin.UndoCommand;
import org.bukkit.command.*;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

public class CommandManager implements CommandExecutor {
    private final JavaPlugin plugin;
    private final Map<String, AuroraCommand> commands;
    private final ArgumentTypeRegistry argumentRegistry;
    private final CooldownManager cooldownManager;
    private final ConfirmationManager confirmationManager;
    private final RateLimiter rateLimiter;
    private final TemplateRegistry templateRegistry;
    private final CommandMetrics commandMetrics;
    private final ABTestManager abTestManager;
    private final TransactionManager transactionManager;
    private final ScriptEngine scriptEngine;
    private final CommandGUI commandGUI;
    private final CommandLoader commandLoader;
    private final CommandPipeline commandPipeline;
    private final NLPParser nlpParser;
    private final NLPChatListener nlpChatListener;
    private final CommandHistory commandHistory;

    public CommandManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.commands = new HashMap<>();
        this.argumentRegistry = new ArgumentTypeRegistry();
        this.cooldownManager = CooldownManager.getInstance(plugin);
        this.confirmationManager = ConfirmationManager.getInstance(plugin);
        this.rateLimiter = new RateLimiter();
        this.templateRegistry = new TemplateRegistry();
        this.commandMetrics = new CommandMetrics();
        this.abTestManager = new ABTestManager();
        this.transactionManager = new TransactionManager();
        this.scriptEngine = new ScriptEngine();
        this.commandGUI = new CommandGUI(plugin);
        this.commandLoader = new CommandLoader(this);
        this.commandPipeline = new CommandPipeline(this);
        this.nlpParser = new NLPParser("aurora");
        this.nlpChatListener = new NLPChatListener(nlpParser, plugin);
        plugin.getServer().getPluginManager().registerEvents(nlpChatListener, plugin);
        this.commandHistory = CommandHistory.getInstance(plugin);
        registerDefaultArgumentTypes();

        // Register built-in commands
        StatsCommand.register(this, commandMetrics);
        UndoCommand.register(this, commandHistory);

        // Display startup banner
        plugin.getLogger().info("┌─────────────────────────────────────┐");
        plugin.getLogger().info("│   Aurora Commands v2.0.0            │");
        plugin.getLogger().info("│   Enterprise Command Framework      │");
        plugin.getLogger().info("└─────────────────────────────────────┘");
        plugin.getLogger().info("Initialized for: " + plugin.getName());
        plugin.getLogger().info("Registered " + argumentRegistry.getTypes().size() + " argument types");
    }

    private void registerDefaultArgumentTypes() {
        // Player types
        argumentRegistry.registerType("player", new OnlinePlayerArgumentType());
        argumentRegistry.registerType("admins", new AdminPlayerArgumentType());
        argumentRegistry.registerType("offlinePlayer", new OfflinePlayerArgumentType());

        // String types
        argumentRegistry.registerType("string", new StringArgumentType());
        argumentRegistry.registerType("greedyString", new GreedyStringArgumentType());

        // Number types
        argumentRegistry.registerType("integer", new IntegerArgumentType());
        argumentRegistry.registerType("float", new FloatArgumentType());
        argumentRegistry.registerType("double", new DoubleArgumentType());
        argumentRegistry.registerType("boolean", new BooleanArgumentType());

        // Game object types
        argumentRegistry.registerType("location", new LocationArgumentType());
        argumentRegistry.registerType("world", new WorldArgumentType());
        argumentRegistry.registerType("entityType", new EntityArgumentType());

        // Item types
        argumentRegistry.registerType("item", new ItemStackArgumentType());
        argumentRegistry.registerType("itemData", new ItemDataArgumentType());

        // Effect types
        argumentRegistry.registerType("enchantment", new EnchantmentArgumentType());
        argumentRegistry.registerType("potionEffect", new PotionEffectTypeArgumentType());
        argumentRegistry.registerType("sound", new SoundArgumentType());

        // Utility types
        argumentRegistry.registerType("color", new ColorArgumentType());
        argumentRegistry.registerType("time", new TimeArgumentType());
        argumentRegistry.registerType("timeTicks", new TimeArgumentType(true));

        plugin.getLogger().info("Registered " + argumentRegistry.getTypes().size() + " default argument types");
    }

    public void registerCommand(AuroraCommand command) {
        // Auto-add metrics middleware to track command execution
        command.addMiddleware(new MetricsMiddleware(commandMetrics));

        commands.put(command.getName().toLowerCase(), command);
        for (String alias : command.getAliases()) {
            commands.put(alias.toLowerCase(), command);
        }
        PluginCommand pluginCommand = plugin.getCommand(command.getName());
        if (pluginCommand != null) {
            pluginCommand.setExecutor(this);
            pluginCommand.setTabCompleter(new CommandTabCompleter(commands, argumentRegistry));
            plugin.getLogger().info("Successfully registered command: " + command.getName());
        } else {
            plugin.getLogger().warning("PluginCommand null for: " + command.getName() + ". Attempting manual registration.");
            try {
                Constructor<PluginCommand> constructor = PluginCommand.class.getDeclaredConstructor(String.class, Plugin.class);
                constructor.setAccessible(true);
                pluginCommand = constructor.newInstance(command.getName(), plugin);
                pluginCommand.setAliases(command.getAliases());
                pluginCommand.setExecutor(this);
                pluginCommand.setTabCompleter(new CommandTabCompleter(commands, argumentRegistry));
                Field commandMapField = plugin.getServer().getPluginManager().getClass().getDeclaredField("commandMap");
                commandMapField.setAccessible(true);
                SimpleCommandMap commandMap = (SimpleCommandMap) commandMapField.get(plugin.getServer().getPluginManager());
                commandMap.register(plugin.getName(), pluginCommand);
                plugin.getLogger().info("Manually registered command: " + command.getName());
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to manually register command: " + command.getName());
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        plugin.getLogger().info("Processing command: " + command.getName() + " with label: " + label);

        // Check for pipeline syntax
        if (args != null && args.length > 0) {
            String fullArgs = String.join(" ", args);
            if (commandPipeline.isPipeline(fullArgs)) {
                // This is a pipeline command
                String fullCommand = command.getName() + " " + fullArgs;
                plugin.getLogger().info("Detected pipeline command: " + fullCommand);

                // Parse pipeline
                List<PipelineStage> stages = commandPipeline.parse(sender, fullCommand);
                if (stages == null) {
                    return true; // Error already sent to sender
                }

                // Validate pipeline
                if (!commandPipeline.validate(sender, stages)) {
                    return true; // Error already sent to sender
                }

                // Execute pipeline
                PipelineExecutor executor = new PipelineExecutor();
                executor.execute(sender, stages);
                return true;
            }
        }

        AuroraCommand auroraCommand = commands.get(command.getName().toLowerCase());
        if (auroraCommand == null) {
            plugin.getLogger().warning("No AuroraCommand found for: " + command.getName());

            // Suggest similar commands
            List<String> availableCommands = commands.values().stream()
                .map(AuroraCommand::getName)
                .distinct()
                .filter(cmdName -> {
                    AuroraCommand cmd = commands.get(cmdName.toLowerCase());
                    return cmd != null && cmd.hasPermission(sender);
                })
                .collect(Collectors.toList());

            if (!availableCommands.isEmpty()) {
                String suggestion = CommandSuggester.createSuggestionMessage(command.getName(), availableCommands);
                sender.sendMessage(suggestion);
                return true;
            }

            return false;
        }

        if (!auroraCommand.hasPermission(sender)) {
            sender.sendMessage("§cYou don't have permission!");
            return true;
        }

        if (auroraCommand.isOnCooldown(sender)) {
            long remaining = auroraCommand.getCooldownRemaining(sender);
            sender.sendMessage("§cCommand on cooldown! Wait " + (remaining / 1000) + " seconds.");
            return true;
        }

        try {
            auroraCommand.execute(sender, args);
        } catch (ArgumentParseException e) {
            sender.sendMessage("§c" + e.getMessage());
        }

        auroraCommand.applyCooldown(sender);
        return true;
    }

    public ArgumentTypeRegistry getArgumentRegistry() {
        return argumentRegistry;
    }

    public CooldownManager getCooldownManager() {
        return cooldownManager;
    }

    public JavaPlugin getPlugin() {
        return plugin;
    }

    /**
     * Unregisters a command, allowing it to be re-registered.
     * Useful for command reloading during development.
     *
     * @param commandName The name of the command to unregister
     * @return True if the command was unregistered, false if not found
     */
    public boolean unregisterCommand(String commandName) {
        AuroraCommand command = commands.remove(commandName.toLowerCase());
        if (command != null) {
            // Remove all aliases too
            for (String alias : command.getAliases()) {
                commands.remove(alias.toLowerCase());
            }
            plugin.getLogger().info("Unregistered command: " + commandName);
            return true;
        }
        return false;
    }

    /**
     * Gets all registered commands.
     *
     * @return Map of command names to AuroraCommand instances
     */
    public Map<String, AuroraCommand> getCommands() {
        return new HashMap<>(commands);
    }

    /**
     * Gets the confirmation manager.
     *
     * @return The ConfirmationManager instance
     */
    public ConfirmationManager getConfirmationManager() {
        return confirmationManager;
    }

    /**
     * Gets the rate limiter.
     *
     * @return The RateLimiter instance
     */
    public RateLimiter getRateLimiter() {
        return rateLimiter;
    }

    /**
     * Gets the template registry.
     *
     * @return The TemplateRegistry instance
     */
    public TemplateRegistry getTemplateRegistry() {
        return templateRegistry;
    }

    /**
     * Gets the command metrics.
     *
     * @return The CommandMetrics instance
     */
    public CommandMetrics getCommandMetrics() {
        return commandMetrics;
    }

    /**
     * Gets the A/B test manager.
     *
     * @return The ABTestManager instance
     */
    public ABTestManager getABTestManager() {
        return abTestManager;
    }

    /**
     * Gets the transaction manager.
     *
     * @return The TransactionManager instance
     */
    public TransactionManager getTransactionManager() {
        return transactionManager;
    }

    /**
     * Gets the script engine.
     *
     * @return The ScriptEngine instance
     */
    public ScriptEngine getScriptEngine() {
        return scriptEngine;
    }

    /**
     * Gets the command GUI manager.
     *
     * @return The CommandGUI instance
     */
    public CommandGUI getCommandGUI() {
        return commandGUI;
    }

    /**
     * Gets the command loader.
     *
     * @return The CommandLoader instance
     */
    public CommandLoader getCommandLoader() {
        return commandLoader;
    }

    /**
     * Gets the command pipeline manager.
     *
     * @return The CommandPipeline instance
     */
    public CommandPipeline getCommandPipeline() {
        return commandPipeline;
    }

    /**
     * Gets the NLP parser.
     *
     * @return The NLPParser instance
     */
    public NLPParser getNLPParser() {
        return nlpParser;
    }

    /**
     * Gets the NLP chat listener.
     *
     * @return The NLPChatListener instance
     */
    public NLPChatListener getNLPChatListener() {
        return nlpChatListener;
    }

    /**
     * Enable or disable natural language processing.
     *
     * @param enabled True to enable, false to disable
     */
    public void setNLPEnabled(boolean enabled) {
        if (enabled) {
            nlpChatListener.enable();
        } else {
            nlpChatListener.disable();
        }
        plugin.getLogger().info("NLP " + (enabled ? "enabled" : "disabled"));
    }

    /**
     * Gets the command history manager.
     *
     * @return The CommandHistory instance
     */
    public CommandHistory getCommandHistory() {
        return commandHistory;
    }

    /**
     * Load commands from a directory containing YAML/JSON files.
     * Each file should contain a command definition.
     *
     * @param directory The directory to load commands from
     * @return List of successfully loaded command names
     */
    public List<String> loadCommandsFromDirectory(File directory) {
        return commandLoader.loadCommandsFromDirectory(directory);
    }

    /**
     * Load commands from a directory path.
     *
     * @param directoryPath The path to the directory
     * @return List of successfully loaded command names
     */
    public List<String> loadCommandsFromDirectory(String directoryPath) {
        return loadCommandsFromDirectory(new File(directoryPath));
    }

    /**
     * Shuts down the CommandManager and cleanup resources.
     * Should be called when the plugin is disabled.
     */
    public void shutdown() {
        cooldownManager.shutdown();
        confirmationManager.shutdown();
        rateLimiter.shutdown();
        plugin.getLogger().info("Aurora Commands framework shut down successfully");
    }
}