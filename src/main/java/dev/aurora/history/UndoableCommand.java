package dev.aurora.history;

import dev.aurora.struct.CommandContext;
import org.bukkit.command.CommandSender;

/**
 * Interface for commands that can be undone.
 * Commands implementing this interface can provide custom undo logic.
 */
@FunctionalInterface
public interface UndoableCommand {

    /**
     * Create an undo action for a command execution.
     * This method is called AFTER the command executes successfully.
     *
     * @param sender The command sender
     * @param context The command context
     * @return A Runnable that reverses the command's effects, or null if cannot be undone
     */
    Runnable createUndoAction(CommandSender sender, CommandContext context);
}
