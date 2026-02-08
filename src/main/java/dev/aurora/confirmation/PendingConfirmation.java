package dev.aurora.confirmation;

import dev.aurora.struct.CommandContext;
import org.bukkit.command.CommandSender;

/**
 * Represents a pending command confirmation.
 * Stores the action to execute and when it expires.
 */
public class PendingConfirmation {
    private final Runnable action;
    private final long expiresAt;
    private final CommandSender sender;
    private final String commandName;
    private final CommandContext context;

    public PendingConfirmation(Runnable action, long timeoutSeconds, CommandSender sender,
                               String commandName, CommandContext context) {
        this.action = action;
        this.expiresAt = System.currentTimeMillis() + (timeoutSeconds * 1000);
        this.sender = sender;
        this.commandName = commandName;
        this.context = context;
    }

    /**
     * Execute the pending action.
     */
    public void execute() {
        action.run();
    }

    /**
     * Check if this confirmation has expired.
     *
     * @return True if expired
     */
    public boolean isExpired() {
        return System.currentTimeMillis() > expiresAt;
    }

    /**
     * Get remaining time in seconds.
     *
     * @return Seconds until expiration
     */
    public long getRemainingSeconds() {
        long remaining = (expiresAt - System.currentTimeMillis()) / 1000;
        return Math.max(0, remaining);
    }

    public CommandSender getSender() {
        return sender;
    }

    public String getCommandName() {
        return commandName;
    }

    public CommandContext getContext() {
        return context;
    }
}
