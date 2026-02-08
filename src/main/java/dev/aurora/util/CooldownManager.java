package dev.aurora.util;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * Manages command cooldowns with automatic cleanup to prevent memory leaks.
 * Thread-safe singleton implementation.
 */
public class CooldownManager {
    private static CooldownManager instance;
    private final Map<UUID, Map<String, Long>> cooldowns;
    private final ScheduledExecutorService cleanupScheduler;
    private final Logger logger;

    /**
     * Private constructor for singleton pattern.
     */
    private CooldownManager(JavaPlugin plugin) {
        this.cooldowns = new ConcurrentHashMap<>();
        this.cleanupScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "AuroraCommands-CooldownCleanup");
            thread.setDaemon(true);
            return thread;
        });
        this.logger = plugin.getLogger();

        // Schedule cleanup every 5 minutes
        cleanupScheduler.scheduleAtFixedRate(
                this::cleanupExpiredCooldowns,
                5, 5, TimeUnit.MINUTES
        );

        logger.info("CooldownManager initialized with automatic cleanup every 5 minutes");
    }

    /**
     * Gets the singleton instance of CooldownManager.
     *
     * @param plugin The JavaPlugin instance
     * @return The CooldownManager instance
     */
    public static synchronized CooldownManager getInstance(JavaPlugin plugin) {
        if (instance == null) {
            instance = new CooldownManager(plugin);
        }
        return instance;
    }

    /**
     * Checks if a command is on cooldown for a player.
     *
     * @param player        The player to check
     * @param commandName   The command name
     * @param cooldownMillis The cooldown duration in milliseconds
     * @return True if the command is on cooldown, false otherwise
     */
    public boolean isOnCooldown(Player player, String commandName, long cooldownMillis) {
        if (cooldownMillis <= 0) {
            return false;
        }

        UUID uuid = player.getUniqueId();
        Map<String, Long> playerCooldowns = cooldowns.get(uuid);

        if (playerCooldowns == null) {
            return false;
        }

        Long lastUsed = playerCooldowns.get(commandName);
        if (lastUsed == null) {
            return false;
        }

        long now = System.currentTimeMillis();
        return now < lastUsed + cooldownMillis;
    }

    /**
     * Gets the remaining cooldown time for a command.
     *
     * @param player        The player to check
     * @param commandName   The command name
     * @param cooldownMillis The cooldown duration in milliseconds
     * @return The remaining cooldown time in milliseconds, or 0 if not on cooldown
     */
    public long getCooldownRemaining(Player player, String commandName, long cooldownMillis) {
        if (cooldownMillis <= 0) {
            return 0;
        }

        UUID uuid = player.getUniqueId();
        Map<String, Long> playerCooldowns = cooldowns.get(uuid);

        if (playerCooldowns == null) {
            return 0;
        }

        Long lastUsed = playerCooldowns.get(commandName);
        if (lastUsed == null) {
            return 0;
        }

        long now = System.currentTimeMillis();
        long remaining = lastUsed + cooldownMillis - now;
        return Math.max(0, remaining);
    }

    /**
     * Applies a cooldown to a command for a player.
     *
     * @param player      The player
     * @param commandName The command name
     */
    public void applyCooldown(Player player, String commandName) {
        UUID uuid = player.getUniqueId();
        cooldowns.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>())
                .put(commandName, System.currentTimeMillis());
    }

    /**
     * Cleans up expired cooldowns to prevent memory leaks.
     * This method is called automatically every 5 minutes.
     */
    public void cleanupExpiredCooldowns() {
        long now = System.currentTimeMillis();
        int removedPlayers = 0;
        AtomicInteger removedCooldowns = new AtomicInteger();

        // Iterate through all players
        for (Map.Entry<UUID, Map<String, Long>> entry : cooldowns.entrySet()) {
            UUID uuid = entry.getKey();
            Map<String, Long> playerCooldowns = entry.getValue();

            // Remove expired cooldowns for this player
            playerCooldowns.entrySet().removeIf(cooldownEntry -> {
                // Remove if cooldown expired more than 5 minutes ago (generous buffer)
                boolean expired = now > cooldownEntry.getValue() + TimeUnit.MINUTES.toMillis(5);
                if (expired) {
                    removedCooldowns.getAndIncrement();
                }
                return expired;
            });

            // Remove player entry if they have no active cooldowns
            if (playerCooldowns.isEmpty()) {
                cooldowns.remove(uuid);
                removedPlayers++;
            }
        }

        if (removedPlayers > 0 || removedCooldowns.get() > 0) {
            logger.info("Cooldown cleanup: removed " + removedCooldowns + " expired cooldowns and " +
                    removedPlayers + " empty player entries. Active players: " + cooldowns.size());
        }
    }

    /**
     * Manually removes all cooldowns for a specific player.
     * Useful when a player leaves the server.
     *
     * @param uuid The player's UUID
     */
    public void removeCooldowns(UUID uuid) {
        cooldowns.remove(uuid);
    }

    /**
     * Clears all cooldowns for a specific command across all players.
     * Useful for debugging or admin commands.
     *
     * @param commandName The command name
     */
    public void clearCommandCooldowns(String commandName) {
        for (Map<String, Long> playerCooldowns : cooldowns.values()) {
            playerCooldowns.remove(commandName);
        }
        logger.info("Cleared all cooldowns for command: " + commandName);
    }

    /**
     * Clears all cooldowns for all commands and all players.
     * Use with caution.
     */
    public void clearAllCooldowns() {
        int count = cooldowns.size();
        cooldowns.clear();
        logger.info("Cleared all cooldowns for " + count + " players");
    }

    /**
     * Gets the current number of players with active cooldowns.
     *
     * @return The number of players
     */
    public int getActivePlayers() {
        return cooldowns.size();
    }

    /**
     * Shuts down the cleanup scheduler.
     * Should be called when the plugin is disabled.
     */
    public void shutdown() {
        cleanupScheduler.shutdown();
        try {
            if (!cleanupScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                cleanupScheduler.shutdownNow();
            }
            logger.info("CooldownManager shut down successfully");
        } catch (InterruptedException e) {
            cleanupScheduler.shutdownNow();
            Thread.currentThread().interrupt();
            logger.warning("CooldownManager shutdown interrupted");
        }
    }
}
