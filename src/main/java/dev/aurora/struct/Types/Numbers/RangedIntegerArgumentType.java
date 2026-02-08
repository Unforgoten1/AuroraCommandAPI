package dev.aurora.struct.Types.Numbers;

import dev.aurora.exception.ArgumentParseException;
import dev.aurora.struct.ArgumentType;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

/**
 * ArgumentType for parsing integers with minimum and maximum bounds.
 * Useful for health values, percentages, levels, etc.
 *
 * Example: new RangedIntegerArgumentType(0, 100) for percentages
 */
public class RangedIntegerArgumentType implements ArgumentType<Integer> {
    private final int min;
    private final int max;

    /**
     * Creates a RangedIntegerArgumentType with specified bounds.
     *
     * @param min The minimum allowed value (inclusive)
     * @param max The maximum allowed value (inclusive)
     */
    public RangedIntegerArgumentType(int min, int max) {
        if (min > max) {
            throw new IllegalArgumentException("Minimum value (" + min + ") cannot be greater than maximum value (" + max + ")");
        }
        this.min = min;
        this.max = max;
    }

    @Override
    public String getName() {
        return "rangedInteger[" + min + "," + max + "]";
    }

    @Override
    public Integer parse(CommandSender sender, String input) throws ArgumentParseException {
        if (input == null || input.trim().isEmpty()) {
            throw new ArgumentParseException("Number cannot be empty!");
        }

        try {
            int value = Integer.parseInt(input);

            if (value < min || value > max) {
                throw new ArgumentParseException(
                        "Value must be between " + min + " and " + max + " (got " + value + ")",
                        "rangedInteger", input
                );
            }

            return value;
        } catch (NumberFormatException e) {
            throw new ArgumentParseException(
                    "'" + input + "' is not a valid integer!",
                    "rangedInteger", input
            );
        }
    }

    @Override
    public List<String> getCompletions(CommandSender sender) {
        List<String> completions = new ArrayList<>();

        // Add min and max as suggestions
        completions.add(String.valueOf(min));
        if (max != min) {
            completions.add(String.valueOf(max));
        }

        // Add middle value if range is reasonable
        if (max - min <= 100 && max - min > 1) {
            int middle = (min + max) / 2;
            completions.add(String.valueOf(middle));
        }

        // Add quarter points for larger ranges
        if (max - min > 10) {
            int quarter = min + (max - min) / 4;
            int threeQuarter = max - (max - min) / 4;
            completions.add(String.valueOf(quarter));
            completions.add(String.valueOf(threeQuarter));
        }

        return completions;
    }

    public int getMin() {
        return min;
    }

    public int getMax() {
        return max;
    }
}
