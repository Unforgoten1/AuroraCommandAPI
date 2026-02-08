package dev.aurora.struct.Types.Numbers;

import dev.aurora.exception.ArgumentParseException;
import dev.aurora.struct.ArgumentType;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

/**
 * ArgumentType for parsing doubles with minimum and maximum bounds.
 * Useful for speeds, multipliers, coordinates, etc.
 *
 * Example: new RangedDoubleArgumentType(0.0, 1.0) for multipliers
 */
public class RangedDoubleArgumentType implements ArgumentType<Double> {
    private final double min;
    private final double max;

    /**
     * Creates a RangedDoubleArgumentType with specified bounds.
     *
     * @param min The minimum allowed value (inclusive)
     * @param max The maximum allowed value (inclusive)
     */
    public RangedDoubleArgumentType(double min, double max) {
        if (min > max) {
            throw new IllegalArgumentException("Minimum value (" + min + ") cannot be greater than maximum value (" + max + ")");
        }
        this.min = min;
        this.max = max;
    }

    @Override
    public String getName() {
        return "rangedDouble[" + min + "," + max + "]";
    }

    @Override
    public Double parse(CommandSender sender, String input) throws ArgumentParseException {
        if (input == null || input.trim().isEmpty()) {
            throw new ArgumentParseException("Number cannot be empty!");
        }

        try {
            double value = Double.parseDouble(input);

            if (value < min || value > max) {
                throw new ArgumentParseException(
                        "Value must be between " + min + " and " + max + " (got " + value + ")",
                        "rangedDouble", input
                );
            }

            return value;
        } catch (NumberFormatException e) {
            throw new ArgumentParseException(
                    "'" + input + "' is not a valid number!",
                    "rangedDouble", input
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

        // Add middle value
        double middle = (min + max) / 2.0;
        completions.add(String.valueOf(middle));

        // Add quarter points
        double quarter = min + (max - min) / 4.0;
        double threeQuarter = max - (max - min) / 4.0;
        completions.add(String.valueOf(quarter));
        completions.add(String.valueOf(threeQuarter));

        return completions;
    }

    public double getMin() {
        return min;
    }

    public double getMax() {
        return max;
    }
}
