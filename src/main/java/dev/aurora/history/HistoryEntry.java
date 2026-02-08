package dev.aurora.history;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a single command execution in history.
 * Stores the command, arguments, and optional undo action.
 */
public class HistoryEntry {
    private final String commandName;
    private final String[] args;
    private final long timestamp;
    private final Map<String, Object> context;
    private final Runnable undoAction;
    private boolean undone;

    /**
     * Create a history entry.
     *
     * @param commandName The command name
     * @param args The command arguments
     * @param undoAction Optional undo action (can be null)
     */
    public HistoryEntry(String commandName, String[] args, Runnable undoAction) {
        this.commandName = commandName;
        this.args = args != null ? args.clone() : new String[0];
        this.timestamp = System.currentTimeMillis();
        this.context = new HashMap<>();
        this.undoAction = undoAction;
        this.undone = false;
    }

    /**
     * Get the command name.
     *
     * @return Command name
     */
    public String getCommandName() {
        return commandName;
    }

    /**
     * Get the command arguments.
     *
     * @return Arguments array
     */
    public String[] getArgs() {
        return args.clone();
    }

    /**
     * Get the timestamp when this command was executed.
     *
     * @return Timestamp in milliseconds
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Get time elapsed since execution.
     *
     * @return Elapsed time in milliseconds
     */
    public long getElapsedTime() {
        return System.currentTimeMillis() - timestamp;
    }

    /**
     * Check if this command can be undone.
     *
     * @return True if undo action is available
     */
    public boolean isUndoable() {
        return undoAction != null && !undone;
    }

    /**
     * Check if this command has been undone.
     *
     * @return True if undone
     */
    public boolean isUndone() {
        return undone;
    }

    /**
     * Execute the undo action.
     *
     * @return True if undo was successful
     */
    public boolean undo() {
        if (!isUndoable()) {
            return false;
        }

        try {
            undoAction.run();
            undone = true;
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Store contextual data with this entry.
     *
     * @param key The key
     * @param value The value
     */
    public void setContext(String key, Object value) {
        context.put(key, value);
    }

    /**
     * Get contextual data from this entry.
     *
     * @param key The key
     * @return The value, or null if not found
     */
    public Object getContext(String key) {
        return context.get(key);
    }

    /**
     * Get the full command string for display.
     *
     * @return Command string
     */
    public String getCommandString() {
        if (args.length == 0) {
            return "/" + commandName;
        }
        return "/" + commandName + " " + String.join(" ", args);
    }

    @Override
    public String toString() {
        return "HistoryEntry{" +
               "command='" + getCommandString() + "', " +
               "timestamp=" + timestamp + ", " +
               "undoable=" + isUndoable() + ", " +
               "undone=" + undone +
               "}";
    }
}
