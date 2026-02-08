package dev.aurora.pipeline;

import dev.aurora.Command.AuroraCommand;
import org.bukkit.command.CommandSender;

/**
 * Represents a single stage in a command pipeline.
 * Each stage consists of a command and its arguments.
 */
public class PipelineStage {
    private final AuroraCommand command;
    private final String[] args;
    private final String rawCommand;

    /**
     * Create a pipeline stage.
     *
     * @param command The command to execute
     * @param args The arguments for the command
     * @param rawCommand The raw command string (for logging)
     */
    public PipelineStage(AuroraCommand command, String[] args, String rawCommand) {
        this.command = command;
        this.args = args;
        this.rawCommand = rawCommand;
    }

    /**
     * Get the command for this stage.
     *
     * @return The Aurora command
     */
    public AuroraCommand getCommand() {
        return command;
    }

    /**
     * Get the arguments for this stage.
     *
     * @return The arguments array
     */
    public String[] getArgs() {
        return args;
    }

    /**
     * Get the raw command string.
     *
     * @return The raw command
     */
    public String getRawCommand() {
        return rawCommand;
    }

    /**
     * Check if this command has permission.
     *
     * @param sender The command sender
     * @return True if sender has permission
     */
    public boolean hasPermission(CommandSender sender) {
        return command.hasPermission(sender);
    }

    /**
     * Check if this command is on cooldown.
     *
     * @param sender The command sender
     * @return True if on cooldown
     */
    public boolean isOnCooldown(CommandSender sender) {
        return command.isOnCooldown(sender);
    }

    @Override
    public String toString() {
        return "PipelineStage{command=" + command.getName() + ", args=" + java.util.Arrays.toString(args) + "}";
    }
}
