package dev.aurora.struct.Types.World;

import dev.aurora.exception.ArgumentParseException;
import dev.aurora.struct.ArgumentType;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

/**
 * ArgumentType for parsing Bukkit World objects.
 * Matches world names case-insensitively.
 */
public class WorldArgumentType implements ArgumentType<World> {

    @Override
    public String getName() {
        return "world";
    }

    @Override
    public World parse(CommandSender sender, String input) throws ArgumentParseException {
        if (input == null || input.trim().isEmpty()) {
            throw new ArgumentParseException("World name cannot be empty!");
        }

        // Try exact match first
        World world = Bukkit.getWorld(input);
        if (world != null) {
            return world;
        }

        // Try case-insensitive match
        for (World w : Bukkit.getWorlds()) {
            if (w.getName().equalsIgnoreCase(input)) {
                return w;
            }
        }

        throw new ArgumentParseException("World '" + input + "' not found!");
    }

    @Override
    public List<String> getCompletions(CommandSender sender) {
        List<String> completions = new ArrayList<>();
        for (World world : Bukkit.getWorlds()) {
            completions.add(world.getName());
        }
        return completions;
    }
}
