package dev.aurora.history;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Manages command execution history for players.
 * Tracks commands and allows undoing recent actions.
 */
public class CommandHistory {
    private static CommandHistory instance;
    private final Map<UUID, LinkedList<HistoryEntry>> playerHistory;
    private final int maxHistorySize;
    private final Logger logger;
    private final JavaPlugin plugin;

    /**
     * Private constructor for singleton.
     */
    private CommandHistory(JavaPlugin plugin, int maxHistorySize) {
        this.plugin = plugin;
        this.playerHistory = new ConcurrentHashMap<>();
        this.maxHistorySize = maxHistorySize;
        this.logger = Logger.getLogger("AuroraCommands");
    }

    /**
     * Get the singleton instance.
     *
     * @param plugin The plugin instance
     * @return CommandHistory instance
     */
    public static CommandHistory getInstance(JavaPlugin plugin) {
        if (instance == null) {
            synchronized (CommandHistory.class) {
                if (instance == null) {
                    instance = new CommandHistory(plugin, 50); // Default: 50 commands per player
                }
            }
        }
        return instance;
    }

    /**
     * Record a command execution.
     *
     * @param player The player who executed the command
     * @param commandName The command name
     * @param args The command arguments
     * @param undoAction Optional undo action
     */
    public void recordCommand(Player player, String commandName, String[] args, Runnable undoAction) {
        UUID playerId = player.getUniqueId();
        LinkedList<HistoryEntry> history = playerHistory.computeIfAbsent(playerId, k -> new LinkedList<>());

        HistoryEntry entry = new HistoryEntry(commandName, args, undoAction);
        history.addFirst(entry); // Add to front (most recent)

        // Trim history if too large
        while (history.size() > maxHistorySize) {
            history.removeLast();
        }

        logger.fine("Recorded command in history for " + player.getName() + ": " + entry.getCommandString());
    }

    /**
     * Undo the most recent undoable command for a player.
     *
     * @param player The player
     * @return True if undo was successful
     */
    public boolean undo(Player player) {
        UUID playerId = player.getUniqueId();
        LinkedList<HistoryEntry> history = playerHistory.get(playerId);

        if (history == null || history.isEmpty()) {
            return false;
        }

        // Find the most recent undoable command
        for (HistoryEntry entry : history) {
            if (entry.isUndoable()) {
                boolean success = entry.undo();
                if (success) {
                    logger.info("Undid command for " + player.getName() + ": " + entry.getCommandString());
                    return true;
                } else {
                    logger.warning("Failed to undo command for " + player.getName() + ": " + entry.getCommandString());
                    return false;
                }
            }
        }

        return false; // No undoable commands found
    }

    /**
     * Undo a specific command by index (0 = most recent).
     *
     * @param player The player
     * @param index The index (0-based, 0 = most recent)
     * @return True if undo was successful
     */
    public boolean undo(Player player, int index) {
        UUID playerId = player.getUniqueId();
        LinkedList<HistoryEntry> history = playerHistory.get(playerId);

        if (history == null || index < 0 || index >= history.size()) {
            return false;
        }

        HistoryEntry entry = history.get(index);
        if (!entry.isUndoable()) {
            return false;
        }

        boolean success = entry.undo();
        if (success) {
            logger.info("Undid command #" + index + " for " + player.getName() + ": " + entry.getCommandString());
        }
        return success;
    }

    /**
     * Get command history for a player.
     *
     * @param player The player
     * @return List of history entries (most recent first)
     */
    public List<HistoryEntry> getHistory(Player player) {
        UUID playerId = player.getUniqueId();
        LinkedList<HistoryEntry> history = playerHistory.get(playerId);

        if (history == null) {
            return new ArrayList<>();
        }

        return new ArrayList<>(history);
    }

    /**
     * Get recent history (last N commands).
     *
     * @param player The player
     * @param limit Maximum number of entries to return
     * @return List of history entries (most recent first)
     */
    public List<HistoryEntry> getRecentHistory(Player player, int limit) {
        List<HistoryEntry> history = getHistory(player);
        return history.subList(0, Math.min(limit, history.size()));
    }

    /**
     * Clear history for a player.
     *
     * @param player The player
     */
    public void clearHistory(Player player) {
        UUID playerId = player.getUniqueId();
        playerHistory.remove(playerId);
        logger.info("Cleared command history for " + player.getName());
    }

    /**
     * Clear all history.
     */
    public void clearAllHistory() {
        playerHistory.clear();
        logger.info("Cleared all command history");
    }

    /**
     * Get the number of commands in history for a player.
     *
     * @param player The player
     * @return Number of commands
     */
    public int getHistorySize(Player player) {
        UUID playerId = player.getUniqueId();
        LinkedList<HistoryEntry> history = playerHistory.get(playerId);
        return history != null ? history.size() : 0;
    }

    /**
     * Get the most recent undoable command for a player.
     *
     * @param player The player
     * @return The most recent undoable entry, or null if none
     */
    public HistoryEntry getMostRecentUndoable(Player player) {
        UUID playerId = player.getUniqueId();
        LinkedList<HistoryEntry> history = playerHistory.get(playerId);

        if (history == null) {
            return null;
        }

        for (HistoryEntry entry : history) {
            if (entry.isUndoable()) {
                return entry;
            }
        }

        return null;
    }

    /**
     * Check if a player has any undoable commands.
     *
     * @param player The player
     * @return True if there are undoable commands
     */
    public boolean hasUndoableCommands(Player player) {
        return getMostRecentUndoable(player) != null;
    }

    /**
     * Get the maximum history size.
     *
     * @return Max history size
     */
    public int getMaxHistorySize() {
        return maxHistorySize;
    }
}
