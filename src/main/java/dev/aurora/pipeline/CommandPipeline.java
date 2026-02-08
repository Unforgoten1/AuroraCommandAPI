package dev.aurora.pipeline;

import dev.aurora.Command.AuroraCommand;
import dev.aurora.Manager.CommandManager;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

/**
 * Parses and manages command pipelines.
 * Allows chaining commands with the pipe operator (|).
 *
 * Example: /listin world | healall
 */
public class CommandPipeline {
    private final CommandManager manager;
    private final Logger logger;

    public CommandPipeline(CommandManager manager) {
        this.manager = manager;
        this.logger = Logger.getLogger("AuroraCommands");
    }

    /**
     * Check if a command string contains a pipeline.
     *
     * @param fullCommand The full command string (without /)
     * @return True if contains pipe operator
     */
    public boolean isPipeline(String fullCommand) {
        return fullCommand != null && fullCommand.contains("|");
    }

    /**
     * Parse a pipeline command string into stages.
     *
     * @param sender The command sender
     * @param fullCommand The full command string (e.g., "listin world | healall")
     * @return List of pipeline stages, or null if parsing fails
     */
    public List<PipelineStage> parse(CommandSender sender, String fullCommand) {
        if (!isPipeline(fullCommand)) {
            logger.warning("Attempted to parse non-pipeline command: " + fullCommand);
            return null;
        }

        List<PipelineStage> stages = new ArrayList<>();

        // Split by pipe operator
        String[] parts = fullCommand.split("\\|");

        for (int i = 0; i < parts.length; i++) {
            String part = parts[i].trim();
            if (part.isEmpty()) {
                logger.warning("Empty pipeline stage at position " + i);
                sender.sendMessage("§cInvalid pipeline: empty stage at position " + (i + 1));
                return null;
            }

            // Parse the command and args
            String[] tokens = part.split("\\s+");
            String commandName = tokens[0];
            String[] args = tokens.length > 1 ? Arrays.copyOfRange(tokens, 1, tokens.length) : new String[0];

            // Find the command
            AuroraCommand command = findCommand(commandName);
            if (command == null) {
                logger.warning("Unknown command in pipeline: " + commandName);
                sender.sendMessage("§cUnknown command in pipeline: " + commandName);
                return null;
            }

            // Check if command supports pipelines
            if (i > 0 && !command.supportsPipelines()) {
                logger.warning("Command does not support pipelines: " + commandName);
                sender.sendMessage("§cCommand '" + commandName + "' does not support pipelines!");
                return null;
            }

            // Check permissions
            if (!command.hasPermission(sender)) {
                sender.sendMessage("§cYou don't have permission for command: " + commandName);
                return null;
            }

            // Create stage
            PipelineStage stage = new PipelineStage(command, args, part);
            stages.add(stage);
            logger.info("Added pipeline stage " + (i + 1) + ": " + command.getName());
        }

        if (stages.isEmpty()) {
            logger.warning("No valid stages in pipeline");
            return null;
        }

        logger.info("Parsed pipeline with " + stages.size() + " stages");
        return stages;
    }

    /**
     * Find a command by name or alias.
     *
     * @param name The command name
     * @return The command, or null if not found
     */
    private AuroraCommand findCommand(String name) {
        for (AuroraCommand cmd : manager.getCommands().values()) {
            if (cmd.getName().equalsIgnoreCase(name)) {
                return cmd;
            }
            for (String alias : cmd.getAliases()) {
                if (alias.equalsIgnoreCase(name)) {
                    return cmd;
                }
            }
        }
        return null;
    }

    /**
     * Validate that all stages in a pipeline are valid.
     *
     * @param sender The command sender
     * @param stages The pipeline stages
     * @return True if all stages are valid
     */
    public boolean validate(CommandSender sender, List<PipelineStage> stages) {
        if (stages == null || stages.isEmpty()) {
            return false;
        }

        for (int i = 0; i < stages.size(); i++) {
            PipelineStage stage = stages.get(i);

            // Check permissions
            if (!stage.hasPermission(sender)) {
                sender.sendMessage("§cYou don't have permission for: " + stage.getCommand().getName());
                return false;
            }

            // Check cooldowns
            if (stage.isOnCooldown(sender)) {
                long remaining = stage.getCommand().getCooldownRemaining(sender);
                sender.sendMessage("§cCommand '" + stage.getCommand().getName() +
                                 "' is on cooldown! Wait " + (remaining / 1000) + " seconds.");
                return false;
            }

            // Check if command is enabled
            if (!stage.getCommand().isEnabled()) {
                sender.sendMessage("§cCommand '" + stage.getCommand().getName() + "' is currently disabled!");
                return false;
            }

            // Check if pipeline is supported (for non-first stages)
            if (i > 0 && !stage.getCommand().supportsPipelines()) {
                sender.sendMessage("§cCommand '" + stage.getCommand().getName() +
                                 "' does not support being used in pipelines!");
                return false;
            }
        }

        return true;
    }

    /**
     * Get the command manager.
     *
     * @return The CommandManager
     */
    public CommandManager getManager() {
        return manager;
    }
}
