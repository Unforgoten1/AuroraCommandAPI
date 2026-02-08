package dev.aurora.condition.builtin;

import dev.aurora.condition.ExecutionCondition;
import dev.aurora.struct.CommandContext;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Condition that checks the Minecraft time of day.
 * Time values: 0=Dawn, 6000=Noon, 12000=Dusk, 18000=Midnight
 */
public class TimeCondition implements ExecutionCondition {
    private final long minTime;
    private final long maxTime;

    /**
     * Create a time-based condition.
     *
     * @param minTime Minimum time in ticks (0-24000)
     * @param maxTime Maximum time in ticks (0-24000)
     */
    public TimeCondition(long minTime, long maxTime) {
        this.minTime = minTime;
        this.maxTime = maxTime;
    }

    @Override
    public boolean test(CommandSender sender, CommandContext context) {
        if (!(sender instanceof Player)) {
            return true; // Console always passes
        }

        Player player = (Player) sender;
        World world = player.getWorld();
        long currentTime = world.getTime() % 24000; // Normalize to 0-24000

        // Handle wrap-around case (e.g., 20000-2000 for night+dawn)
        if (minTime <= maxTime) {
            return currentTime >= minTime && currentTime < maxTime;
        } else {
            return currentTime >= minTime || currentTime < maxTime;
        }
    }

    @Override
    public String getFailureMessage() {
        String timeRange = formatTime(minTime) + " to " + formatTime(maxTime);
        return "§cThis command only works between " + timeRange + "!";
    }

    @Override
    public String getName() {
        return "Time Condition (" + minTime + "-" + maxTime + ")";
    }

    /**
     * Format time ticks into a readable string.
     *
     * @param ticks Time in ticks
     * @return Formatted time string
     */
    private String formatTime(long ticks) {
        ticks = ticks % 24000;
        if (ticks < 6000) return "dawn";
        if (ticks < 12000) return "day";
        if (ticks < 18000) return "dusk";
        return "night";
    }
}
