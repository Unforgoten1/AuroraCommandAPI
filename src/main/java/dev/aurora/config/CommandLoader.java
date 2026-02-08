package dev.aurora.config;

import dev.aurora.Command.AuroraCommand;
import dev.aurora.Manager.CommandManager;
import dev.aurora.struct.ArgumentType;
import dev.aurora.struct.ArgumentTypeRegistry;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Loads commands from YAML/JSON files in a directory.
 * Automatically registers them with the CommandManager.
 */
public class CommandLoader {
    private final CommandManager manager;
    private final YamlCommandParser parser;
    private final Logger logger;

    public CommandLoader(CommandManager manager) {
        this.manager = manager;
        this.parser = new YamlCommandParser();
        this.logger = Logger.getLogger("AuroraCommands");
    }

    /**
     * Load all commands from a directory.
     *
     * @param directory The directory containing command files
     * @return List of successfully loaded command names
     */
    public List<String> loadCommandsFromDirectory(File directory) {
        List<String> loadedCommands = new ArrayList<>();

        if (!directory.exists()) {
            logger.warning("Commands directory does not exist: " + directory.getPath());
            logger.info("Creating directory: " + directory.getPath());
            directory.mkdirs();
            return loadedCommands;
        }

        if (!directory.isDirectory()) {
            logger.severe("Path is not a directory: " + directory.getPath());
            return loadedCommands;
        }

        File[] files = directory.listFiles((dir, name) ->
            name.toLowerCase().endsWith(".yml") || name.toLowerCase().endsWith(".yaml"));

        if (files == null || files.length == 0) {
            logger.info("No command files found in " + directory.getPath());
            return loadedCommands;
        }

        logger.info("Found " + files.length + " command file(s) in " + directory.getPath());

        for (File file : files) {
            try {
                CommandDefinition def = parser.parse(file);
                if (def != null) {
                    AuroraCommand command = buildCommand(def);
                    if (command != null) {
                        manager.registerCommand(command);
                        loadedCommands.add(def.getName());
                        logger.info("Loaded and registered command: " + def.getName());
                    }
                }
            } catch (Exception e) {
                logger.severe("Failed to load command from " + file.getName() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }

        logger.info("Successfully loaded " + loadedCommands.size() + " command(s) from configuration");
        return loadedCommands;
    }

    /**
     * Build an AuroraCommand from a CommandDefinition.
     *
     * @param def The command definition
     * @return AuroraCommand, or null if build fails
     */
    private AuroraCommand buildCommand(CommandDefinition def) {
        try {
            AuroraCommand command = new AuroraCommand(def.getName(), manager);

            // Set description
            if (def.getDescription() != null) {
                command.setDescription(def.getDescription());
            }

            // Set permission
            if (def.getPermission() != null) {
                command.setPermission(def.getPermission());
            }

            // Add aliases
            for (String alias : def.getAliases()) {
                command.addAlias(alias);
            }

            // Set cooldown
            if (def.getCooldown() > 0) {
                command.setCooldown(def.getCooldown());
            }

            // Set async
            if (def.isAsync()) {
                command.setAsync(true);
            }

            // Add arguments
            ArgumentTypeRegistry registry = manager.getArgumentRegistry();
            for (CommandDefinition.ArgumentDefinition argDef : def.getArguments()) {
                ArgumentType<?> type = registry.getType(argDef.getType());
                if (type != null) {
                    command.addArgument(argDef.getName(), type);
                } else {
                    logger.warning("Unknown argument type '" + argDef.getType() +
                                 "' for argument '" + argDef.getName() + "' in command " + def.getName());
                }
            }

            // Set execution logic
            if (!def.getActions().isEmpty()) {
                ConfigurableExecutor executor = new ConfigurableExecutor(def.getActions());

                if (def.isRequiresPlayer()) {
                    command.addExecution(Player.class, executor);
                } else {
                    command.addExecution(executor);
                }
            }

            // Apply custom messages
            for (Map.Entry<String, String> entry : def.getCustomMessages().entrySet()) {
                command.setCustomMessage(entry.getKey(), entry.getValue());
            }

            return command;

        } catch (Exception e) {
            logger.severe("Failed to build command " + def.getName() + ": " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Load a single command from a file.
     *
     * @param file The command file
     * @return True if loaded successfully
     */
    public boolean loadCommand(File file) {
        try {
            CommandDefinition def = parser.parse(file);
            if (def != null) {
                AuroraCommand command = buildCommand(def);
                if (command != null) {
                    manager.registerCommand(command);
                    logger.info("Loaded command: " + def.getName() + " from " + file.getName());
                    return true;
                }
            }
        } catch (Exception e) {
            logger.severe("Failed to load command from " + file.getName() + ": " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Reload all commands from a directory.
     * Unregisters existing commands and loads fresh from files.
     *
     * @param directory The directory containing command files
     * @return List of successfully reloaded command names
     */
    public List<String> reloadCommands(File directory) {
        // Note: This would require tracking which commands were loaded from files
        // For now, just load them (they may override existing commands)
        logger.info("Reloading commands from " + directory.getPath());
        return loadCommandsFromDirectory(directory);
    }
}
