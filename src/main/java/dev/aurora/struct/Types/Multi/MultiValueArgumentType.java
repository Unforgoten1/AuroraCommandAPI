package dev.aurora.struct.Types.Multi;

import dev.aurora.exception.ArgumentParseException;
import dev.aurora.struct.ArgumentType;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

/**
 * ArgumentType that parses comma-separated lists of values.
 * Wraps another ArgumentType to parse individual elements.
 *
 * Example usage:
 * <pre>
 * // Parse multiple players: "player1,player2,player3"
 * new MultiValueArgumentType<>(new OnlinePlayerArgumentType())
 *
 * // Parse multiple integers: "1,5,10,20"
 * new MultiValueArgumentType<>(new IntegerArgumentType())
 * </pre>
 *
 * @param <T> The type of individual elements
 */
public class MultiValueArgumentType<T> implements ArgumentType<List<T>> {
    private final ArgumentType<T> elementType;
    private final String separator;
    private final int minValues;
    private final int maxValues;

    /**
     * Creates a MultiValueArgumentType with comma separator and no limits.
     *
     * @param elementType The ArgumentType used to parse individual elements
     */
    public MultiValueArgumentType(ArgumentType<T> elementType) {
        this(elementType, ",", 1, Integer.MAX_VALUE);
    }

    /**
     * Creates a MultiValueArgumentType with custom separator.
     *
     * @param elementType The ArgumentType used to parse individual elements
     * @param separator   The separator character/string (e.g., "," or "|")
     */
    public MultiValueArgumentType(ArgumentType<T> elementType, String separator) {
        this(elementType, separator, 1, Integer.MAX_VALUE);
    }

    /**
     * Creates a MultiValueArgumentType with custom separator and value limits.
     *
     * @param elementType The ArgumentType used to parse individual elements
     * @param separator   The separator character/string (e.g., "," or "|")
     * @param minValues   Minimum number of values required
     * @param maxValues   Maximum number of values allowed
     */
    public MultiValueArgumentType(ArgumentType<T> elementType, String separator, int minValues, int maxValues) {
        if (minValues < 1) {
            throw new IllegalArgumentException("minValues must be at least 1");
        }
        if (maxValues < minValues) {
            throw new IllegalArgumentException("maxValues cannot be less than minValues");
        }
        this.elementType = elementType;
        this.separator = separator;
        this.minValues = minValues;
        this.maxValues = maxValues;
    }

    @Override
    public String getName() {
        return "multiValue<" + elementType.getName() + ">";
    }

    @Override
    public List<T> parse(CommandSender sender, String input) throws ArgumentParseException {
        if (input == null || input.trim().isEmpty()) {
            throw new ArgumentParseException(
                    "Multi-value input cannot be empty!",
                    getName(), input,
                    "Use format: value1" + separator + "value2" + separator + "value3"
            );
        }

        String[] parts = input.split(separator);
        List<T> values = new ArrayList<>();

        // Parse each element
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i].trim();
            if (part.isEmpty()) {
                continue; // Skip empty parts
            }

            try {
                T value = elementType.parse(sender, part);
                values.add(value);
            } catch (ArgumentParseException e) {
                throw new ArgumentParseException(
                        "Invalid element at position " + (i + 1) + ": " + e.getMessage(),
                        getName(), part,
                        e.getSuggestion()
                );
            }
        }

        // Validate count
        if (values.size() < minValues) {
            throw new ArgumentParseException(
                    "Expected at least " + minValues + " values, but got " + values.size(),
                    getName(), input,
                    "Provide at least " + minValues + " values separated by '" + separator + "'"
            );
        }

        if (values.size() > maxValues) {
            throw new ArgumentParseException(
                    "Expected at most " + maxValues + " values, but got " + values.size(),
                    getName(), input,
                    "Provide at most " + maxValues + " values separated by '" + separator + "'"
            );
        }

        return values;
    }

    @Override
    public List<String> getCompletions(CommandSender sender) {
        // Return completions from the element type with separator hints
        List<String> completions = new ArrayList<>();
        List<String> elementCompletions = elementType.getCompletions(sender);

        // Add individual element completions
        completions.addAll(elementCompletions);

        // Add examples with separator for multi-value
        if (elementCompletions.size() >= 2) {
            completions.add(elementCompletions.get(0) + separator + elementCompletions.get(1));
        }

        return completions;
    }

    /**
     * Gets the ArgumentType used for individual elements.
     *
     * @return The element ArgumentType
     */
    public ArgumentType<T> getElementType() {
        return elementType;
    }

    /**
     * Gets the separator used to split values.
     *
     * @return The separator string
     */
    public String getSeparator() {
        return separator;
    }

    /**
     * Gets the minimum number of values required.
     *
     * @return The minimum value count
     */
    public int getMinValues() {
        return minValues;
    }

    /**
     * Gets the maximum number of values allowed.
     *
     * @return The maximum value count
     */
    public int getMaxValues() {
        return maxValues;
    }
}
