package dev.aurora.struct.Types.Enchantment;

import dev.aurora.exception.ArgumentParseException;
import dev.aurora.struct.ArgumentType;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;

import java.util.ArrayList;
import java.util.List;

/**
 * ArgumentType for parsing Bukkit Enchantment objects.
 * Supports enchantment names (case-insensitive).
 */
public class EnchantmentArgumentType implements ArgumentType<Enchantment> {

    @Override
    public String getName() {
        return "enchantment";
    }

    @Override
    public Enchantment parse(CommandSender sender, String input) throws ArgumentParseException {
        if (input == null || input.trim().isEmpty()) {
            throw new ArgumentParseException("Enchantment name cannot be empty!");
        }

        // Try to get enchantment by name
        Enchantment enchantment = Enchantment.getByName(input.toUpperCase());
        if (enchantment != null) {
            return enchantment;
        }

        // Try case-insensitive match with all registered enchantments
        for (Enchantment ench : Enchantment.values()) {
            if (ench.getName().equalsIgnoreCase(input)) {
                return ench;
            }
        }

        throw new ArgumentParseException("Enchantment '" + input + "' not found!");
    }

    @Override
    public List<String> getCompletions(CommandSender sender) {
        List<String> completions = new ArrayList<>();
        for (Enchantment enchantment : Enchantment.values()) {
            completions.add(enchantment.getName());
        }
        return completions;
    }
}
