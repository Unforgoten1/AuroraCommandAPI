package dev.aurora.struct.Types.Potion;

import dev.aurora.exception.ArgumentParseException;
import dev.aurora.struct.ArgumentType;
import org.bukkit.command.CommandSender;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;

/**
 * ArgumentType for parsing Bukkit PotionEffectType objects.
 * Supports potion effect names (case-insensitive).
 */
public class PotionEffectTypeArgumentType implements ArgumentType<PotionEffectType> {

    @Override
    public String getName() {
        return "potionEffect";
    }

    @Override
    public PotionEffectType parse(CommandSender sender, String input) throws ArgumentParseException {
        if (input == null || input.trim().isEmpty()) {
            throw new ArgumentParseException("Potion effect name cannot be empty!");
        }

        // Try to get potion effect by name
        PotionEffectType effect = PotionEffectType.getByName(input.toUpperCase());
        if (effect != null) {
            return effect;
        }

        // Try case-insensitive match with all registered effects
        for (PotionEffectType pet : PotionEffectType.values()) {
            if (pet != null && pet.getName().equalsIgnoreCase(input)) {
                return pet;
            }
        }

        throw new ArgumentParseException("Potion effect '" + input + "' not found!");
    }

    @Override
    public List<String> getCompletions(CommandSender sender) {
        List<String> completions = new ArrayList<>();
        for (PotionEffectType effect : PotionEffectType.values()) {
            if (effect != null) {
                completions.add(effect.getName());
            }
        }
        return completions;
    }
}
