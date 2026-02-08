package dev.aurora.condition.builtin;

import dev.aurora.condition.ExecutionCondition;
import dev.aurora.struct.CommandContext;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Condition that checks the weather in the player's world.
 */
public class WeatherCondition implements ExecutionCondition {
    private final boolean requireClear;

    /**
     * Create a weather-based condition.
     *
     * @param requireClear True to require clear weather, false to require rain/storm
     */
    public WeatherCondition(boolean requireClear) {
        this.requireClear = requireClear;
    }

    @Override
    public boolean test(CommandSender sender, CommandContext context) {
        if (!(sender instanceof Player)) {
            return true; // Console always passes
        }

        Player player = (Player) sender;
        World world = player.getWorld();
        boolean isClear = !world.hasStorm();

        return requireClear == isClear;
    }

    @Override
    public String getFailureMessage() {
        if (requireClear) {
            return "§cThis command only works in clear weather!";
        } else {
            return "§cThis command only works during rain or storms!";
        }
    }

    @Override
    public String getName() {
        return "Weather Condition (" + (requireClear ? "clear" : "rain/storm") + ")";
    }
}
