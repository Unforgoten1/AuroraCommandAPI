package dev.aurora.condition.builtin;

import dev.aurora.condition.ExecutionCondition;
import dev.aurora.struct.CommandContext;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Condition that checks if the command is executed in a specific world.
 */
public class WorldCondition implements ExecutionCondition {
    private final Set<String> allowedWorlds;

    /**
     * Create a world-based condition.
     *
     * @param worldNames Allowed world names
     */
    public WorldCondition(String... worldNames) {
        this.allowedWorlds = new HashSet<>(Arrays.asList(worldNames));
    }

    @Override
    public boolean test(CommandSender sender, CommandContext context) {
        if (!(sender instanceof Player)) {
            return true; // Console always passes
        }

        Player player = (Player) sender;
        World world = player.getWorld();
        return allowedWorlds.contains(world.getName());
    }

    @Override
    public String getFailureMessage() {
        if (allowedWorlds.size() == 1) {
            return "§cThis command only works in world: §e" + allowedWorlds.iterator().next();
        }
        return "§cThis command only works in specific worlds: §e" + String.join(", ", allowedWorlds);
    }

    @Override
    public String getName() {
        return "World Condition (" + String.join(", ", allowedWorlds) + ")";
    }
}
