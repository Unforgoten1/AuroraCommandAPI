package dev.aurora.commands.builtin;

import dev.aurora.Command.AuroraCommand;
import dev.aurora.Manager.CommandManager;
import dev.aurora.history.CommandHistory;
import dev.aurora.history.HistoryEntry;
import dev.aurora.struct.Types.Integers.IntegerArgumentType;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Built-in command to undo previous commands.
 * Usage: /undo [index]
 */
public class UndoCommand {

    /**
     * Register the undo command.
     *
     * @param manager The command manager
     * @param history The command history
     */
    public static void register(CommandManager manager, CommandHistory history) {
        new AuroraCommand("undo", manager)
                .setDescription("Undo your last command")
                .addArgument("index", new IntegerArgumentType())
                .requirePlayer()
                .addExecution((sender, context) -> {
                    Player player = (Player) sender;

                    // Check if player has any undoable commands
                    if (!history.hasUndoableCommands(player)) {
                        player.sendMessage("§cNo commands to undo!");
                        return;
                    }

                    // Get index if provided
                    Integer index = (Integer) context.getArgument("index");

                    boolean success;
                    if (index != null) {
                        // Undo specific command
                        success = history.undo(player, index);
                        if (success) {
                            player.sendMessage("§aUndid command #" + index + "!");
                        } else {
                            player.sendMessage("§cCould not undo command #" + index + "!");
                        }
                    } else {
                        // Undo most recent
                        HistoryEntry entry = history.getMostRecentUndoable(player);
                        success = history.undo(player);

                        if (success) {
                            player.sendMessage("§aUndid: §7" + entry.getCommandString());
                        } else {
                            player.sendMessage("§cCould not undo command!");
                        }
                    }
                })
                .register();

        // Also register /history command
        new AuroraCommand("history", manager)
                .setDescription("View your command history")
                .addAlias("cmdhist")
                .addAlias("commandhistory")
                .requirePlayer()
                .addExecution((sender, context) -> {
                    Player player = (Player) sender;
                    List<HistoryEntry> entries = history.getRecentHistory(player, 10);

                    if (entries.isEmpty()) {
                        player.sendMessage("§7No command history.");
                        return;
                    }

                    player.sendMessage("§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                    player.sendMessage("§6§lCommand History §7(Recent 10)");
                    player.sendMessage("§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

                    for (int i = 0; i < entries.size(); i++) {
                        HistoryEntry entry = entries.get(i);
                        long secondsAgo = entry.getElapsedTime() / 1000;

                        String prefix;
                        if (entry.isUndone()) {
                            prefix = "§8§m"; // Strikethrough for undone
                        } else if (entry.isUndoable()) {
                            prefix = "§a"; // Green for undoable
                        } else {
                            prefix = "§7"; // Gray for non-undoable
                        }

                        String status = "";
                        if (entry.isUndone()) {
                            status = " §8[UNDONE]";
                        } else if (entry.isUndoable()) {
                            status = " §a[Can undo]";
                        }

                        player.sendMessage("§e" + i + ". " + prefix + entry.getCommandString() +
                                         " §8(" + secondsAgo + "s ago)" + status);
                    }

                    player.sendMessage("§7Use §e/undo §7to undo last command");
                    player.sendMessage("§7Use §e/undo <number> §7to undo specific command");
                })
                .register();

        manager.getPlugin().getLogger().info("Registered built-in undo and history commands");
    }
}
