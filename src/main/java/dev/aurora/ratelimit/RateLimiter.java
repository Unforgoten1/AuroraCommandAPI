package dev.aurora.ratelimit;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Manages rate limiting for commands.
 * Supports per-player, per-IP, and global rate limits.
 */
public class RateLimiter {
    private final Map<String, Map<UUID, SlidingWindow>> perPlayerLimits;
    private final Map<String, Map<String, SlidingWindow>> perIPLimits;
    private final Map<String, SlidingWindow> globalLimits;
    private final ScheduledExecutorService cleanupExecutor;
    private final Logger logger;

    public RateLimiter() {
        this.perPlayerLimits = new ConcurrentHashMap<>();
        this.perIPLimits = new ConcurrentHashMap<>();
        this.globalLimits = new ConcurrentHashMap<>();
        this.logger = Logger.getLogger("AuroraCommands");

        // Schedule cleanup every 5 minutes
        this.cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "AuroraCommands-RateLimiter-Cleanup");
            t.setDaemon(true);
            return t;
        });
        cleanupExecutor.scheduleAtFixedRate(this::cleanup, 5, 5, TimeUnit.MINUTES);

        logger.fine("RateLimiter initialized with cleanup every 5 minutes");
    }

    /**
     * Check if a command execution is allowed under rate limits.
     *
     * @param commandName The command name
     * @param sender The command sender
     * @param config The rate limit configuration
     * @return True if allowed, false if rate limited
     */
    public boolean tryAcquire(String commandName, CommandSender sender, RateLimitConfig config) {
        // Check global limit first (most restrictive)
        if (config.hasGlobalLimit()) {
            SlidingWindow globalWindow = globalLimits.computeIfAbsent(
                commandName,
                k -> new SlidingWindow(config.getGlobalRequests(), config.getGlobalWindowMillis())
            );

            if (!globalWindow.wouldAllow()) {
                logger.fine("Global rate limit exceeded for command: " + commandName);
                return false;
            }
        }

        // Check per-IP limit (if sender is a player)
        if (config.hasPerIPLimit() && sender instanceof Player) {
            Player player = (Player) sender;
            String ip = player.getAddress() != null ? player.getAddress().getAddress().getHostAddress() : "unknown";

            Map<String, SlidingWindow> ipLimits = perIPLimits.computeIfAbsent(commandName, k -> new ConcurrentHashMap<>());
            SlidingWindow ipWindow = ipLimits.computeIfAbsent(
                ip,
                k -> new SlidingWindow(config.getPerIPRequests(), config.getPerIPWindowMillis())
            );

            if (!ipWindow.wouldAllow()) {
                logger.fine("Per-IP rate limit exceeded for command: " + commandName + " (IP: " + ip + ")");
                return false;
            }
        }

        // Check per-player limit (if sender is a player)
        if (config.hasPerPlayerLimit() && sender instanceof Player) {
            Player player = (Player) sender;
            UUID playerId = player.getUniqueId();

            Map<UUID, SlidingWindow> playerLimits = perPlayerLimits.computeIfAbsent(commandName, k -> new ConcurrentHashMap<>());
            SlidingWindow playerWindow = playerLimits.computeIfAbsent(
                playerId,
                k -> new SlidingWindow(config.getPerPlayerRequests(), config.getPerPlayerWindowMillis())
            );

            if (!playerWindow.wouldAllow()) {
                logger.fine("Per-player rate limit exceeded for command: " + commandName + " (Player: " + player.getName() + ")");
                return false;
            }
        }

        // All checks passed - acquire permits
        if (config.hasGlobalLimit()) {
            globalLimits.get(commandName).tryAcquire();
        }
        if (config.hasPerIPLimit() && sender instanceof Player) {
            Player player = (Player) sender;
            String ip = player.getAddress() != null ? player.getAddress().getAddress().getHostAddress() : "unknown";
            perIPLimits.get(commandName).get(ip).tryAcquire();
        }
        if (config.hasPerPlayerLimit() && sender instanceof Player) {
            Player player = (Player) sender;
            perPlayerLimits.get(commandName).get(player.getUniqueId()).tryAcquire();
        }

        return true;
    }

    /**
     * Get the time (in seconds) until the next request will be allowed.
     *
     * @param commandName The command name
     * @param sender The command sender
     * @param config The rate limit configuration
     * @return Time until next request in seconds, or 0 if currently allowed
     */
    public long getTimeUntilNextRequest(String commandName, CommandSender sender, RateLimitConfig config) {
        long maxWait = 0;

        // Check global limit
        if (config.hasGlobalLimit() && globalLimits.containsKey(commandName)) {
            long wait = globalLimits.get(commandName).getTimeUntilNextRequest();
            maxWait = Math.max(maxWait, wait);
        }

        // Check per-IP limit
        if (config.hasPerIPLimit() && sender instanceof Player) {
            Player player = (Player) sender;
            String ip = player.getAddress() != null ? player.getAddress().getAddress().getHostAddress() : "unknown";
            if (perIPLimits.containsKey(commandName) && perIPLimits.get(commandName).containsKey(ip)) {
                long wait = perIPLimits.get(commandName).get(ip).getTimeUntilNextRequest();
                maxWait = Math.max(maxWait, wait);
            }
        }

        // Check per-player limit
        if (config.hasPerPlayerLimit() && sender instanceof Player) {
            Player player = (Player) sender;
            if (perPlayerLimits.containsKey(commandName) && perPlayerLimits.get(commandName).containsKey(player.getUniqueId())) {
                long wait = perPlayerLimits.get(commandName).get(player.getUniqueId()).getTimeUntilNextRequest();
                maxWait = Math.max(maxWait, wait);
            }
        }

        return maxWait / 1000; // Convert to seconds
    }

    /**
     * Reset rate limits for a specific command.
     *
     * @param commandName The command name
     */
    public void resetCommand(String commandName) {
        perPlayerLimits.remove(commandName);
        perIPLimits.remove(commandName);
        globalLimits.remove(commandName);
        logger.fine("Reset rate limits for command: " + commandName);
    }

    /**
     * Reset rate limits for a specific player.
     *
     * @param playerId The player's UUID
     */
    public void resetPlayer(UUID playerId) {
        perPlayerLimits.values().forEach(map -> map.remove(playerId));
        logger.fine("Reset rate limits for player: " + playerId);
    }

    /**
     * Clean up expired sliding windows.
     * Called automatically every 5 minutes.
     */
    private void cleanup() {
        int removed = 0;

        // Cleanup player limits
        for (Map<UUID, SlidingWindow> map : perPlayerLimits.values()) {
            int sizeBefore = map.size();
            map.entrySet().removeIf(entry -> entry.getValue().getRequestCount() == 0);
            removed += sizeBefore - map.size();
        }

        // Cleanup IP limits
        for (Map<String, SlidingWindow> map : perIPLimits.values()) {
            int sizeBefore = map.size();
            map.entrySet().removeIf(entry -> entry.getValue().getRequestCount() == 0);
            removed += sizeBefore - map.size();
        }

        // Cleanup global limits
        globalLimits.entrySet().removeIf(entry -> entry.getValue().getRequestCount() == 0);

        if (removed > 0) {
            logger.fine("Cleaned up " + removed + " expired rate limit windows");
        }
    }

    /**
     * Shutdown the rate limiter and cleanup resources.
     */
    public void shutdown() {
        cleanupExecutor.shutdown();
        try {
            if (!cleanupExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                cleanupExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            cleanupExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        logger.fine("RateLimiter shut down successfully");
    }
}
