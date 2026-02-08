package dev.aurora.confirmation;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Manages pending command confirmations.
 * Singleton pattern with automatic cleanup of expired confirmations.
 */
public class ConfirmationManager {
    private static ConfirmationManager instance;
    private final Map<UUID, PendingConfirmation> confirmations;
    private final ScheduledExecutorService cleanupScheduler;
    private final JavaPlugin plugin;

    private ConfirmationManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.confirmations = new ConcurrentHashMap<>();
        this.cleanupScheduler = Executors.newSingleThreadScheduledExecutor();

        // Schedule cleanup task every 30 seconds
        cleanupScheduler.scheduleAtFixedRate(this::cleanupExpired, 30, 30, TimeUnit.SECONDS);
        plugin.getLogger().info("ConfirmationManager initialized with automatic cleanup every 30 seconds");
    }

    /**
     * Get the singleton instance.
     *
     * @param plugin The plugin instance
     * @return The ConfirmationManager instance
     */
    public static synchronized ConfirmationManager getInstance(JavaPlugin plugin) {
        if (instance == null) {
            instance = new ConfirmationManager(plugin);
        }
        return instance;
    }

    /**
     * Require confirmation before executing an action.
     *
     * @param player The player who must confirm
     * @param action The action to execute after confirmation
     * @param timeoutSeconds How long the confirmation is valid
     * @param message The message to show the player
     */
    public void requireConfirmation(Player player, Runnable action, long timeoutSeconds, String message) {
        PendingConfirmation pending = new PendingConfirmation(
            action,
            timeoutSeconds,
            player,
            "unknown", // Command name can be set by caller
            null // Context can be set by caller
        );

        confirmations.put(player.getUniqueId(), pending);
        player.sendMessage(message);
        plugin.getLogger().info("Confirmation required for player " + player.getName() +
                               ", expires in " + timeoutSeconds + " seconds");
    }

    /**
     * Confirm and execute a pending action.
     *
     * @param sender The sender confirming
     * @return True if confirmation was executed, false if no pending confirmation
     */
    public boolean confirm(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can confirm commands!");
            return false;
        }

        Player player = (Player) sender;
        PendingConfirmation pending = confirmations.remove(player.getUniqueId());

        if (pending == null) {
            return false;
        }

        if (pending.isExpired()) {
            sender.sendMessage("§cConfirmation expired! Please run the command again.");
            plugin.getLogger().info("Confirmation expired for player " + player.getName());
            return false;
        }

        // Execute the pending action
        try {
            pending.execute();
            plugin.getLogger().info("Confirmation executed for player " + player.getName());
            return true;
        } catch (Exception e) {
            sender.sendMessage("§cError executing confirmed command: " + e.getMessage());
            plugin.getLogger().severe("Error executing confirmation: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Check if a player has a pending confirmation.
     *
     * @param player The player to check
     * @return True if there's a pending confirmation
     */
    public boolean hasPendingConfirmation(Player player) {
        PendingConfirmation pending = confirmations.get(player.getUniqueId());
        return pending != null && !pending.isExpired();
    }

    /**
     * Cancel a pending confirmation.
     *
     * @param player The player whose confirmation to cancel
     * @return True if a confirmation was cancelled
     */
    public boolean cancelConfirmation(Player player) {
        PendingConfirmation removed = confirmations.remove(player.getUniqueId());
        if (removed != null) {
            plugin.getLogger().info("Cancelled confirmation for player " + player.getName());
            return true;
        }
        return false;
    }

    /**
     * Get the pending confirmation for a player.
     *
     * @param player The player
     * @return The pending confirmation, or null
     */
    public PendingConfirmation getPendingConfirmation(Player player) {
        return confirmations.get(player.getUniqueId());
    }

    /**
     * Clean up expired confirmations.
     */
    private void cleanupExpired() {
        int removed = 0;
        for (Map.Entry<UUID, PendingConfirmation> entry : confirmations.entrySet()) {
            if (entry.getValue().isExpired()) {
                confirmations.remove(entry.getKey());
                removed++;
            }
        }
        if (removed > 0) {
            plugin.getLogger().info("Cleaned up " + removed + " expired confirmation(s)");
        }
    }

    /**
     * Shutdown the confirmation manager.
     * Should be called in onDisable().
     */
    public void shutdown() {
        cleanupScheduler.shutdown();
        try {
            if (!cleanupScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                cleanupScheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            cleanupScheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        confirmations.clear();
        plugin.getLogger().info("ConfirmationManager shut down");
    }

    /**
     * Get the number of pending confirmations.
     *
     * @return Confirmation count
     */
    public int getPendingCount() {
        return confirmations.size();
    }
}
