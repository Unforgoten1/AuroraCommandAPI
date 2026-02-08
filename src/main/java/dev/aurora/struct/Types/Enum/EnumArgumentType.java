package dev.aurora.struct.Types.Enum;

import dev.aurora.exception.ArgumentParseException;
import dev.aurora.struct.ArgumentType;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

/**
 * Generic ArgumentType for parsing any Java enum.
 * Supports case-insensitive matching and underscore/space replacement.
 *
 * Example usage:
 * <pre>
 * new EnumArgumentType<>(GameMode.class)
 * new EnumArgumentType<>(Difficulty.class)
 * new EnumArgumentType<>(WeatherType.class)
 * </pre>
 *
 * @param <T> The enum type to parse
 */
public class EnumArgumentType<T extends Enum<T>> implements ArgumentType<T> {
    private final Class<T> enumClass;
    private final String name;

    /**
     * Creates an EnumArgumentType for the specified enum class.
     *
     * @param enumClass The enum class to parse
     */
    public EnumArgumentType(Class<T> enumClass) {
        this.enumClass = enumClass;
        this.name = "enum<" + enumClass.getSimpleName() + ">";
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public T parse(CommandSender sender, String input) throws ArgumentParseException {
        if (input == null || input.trim().isEmpty()) {
            throw new ArgumentParseException(
                    enumClass.getSimpleName() + " cannot be empty!",
                    name, input
            );
        }

        // Normalize input: uppercase and replace spaces with underscores
        String normalized = input.toUpperCase().replace(" ", "_");

        // Try direct enum valueOf
        try {
            return Enum.valueOf(enumClass, normalized);
        } catch (IllegalArgumentException e) {
            // Try case-insensitive match
            for (T enumConstant : enumClass.getEnumConstants()) {
                if (enumConstant.name().equalsIgnoreCase(normalized)) {
                    return enumConstant;
                }
            }

            // Build helpful error message with valid options
            StringBuilder validOptions = new StringBuilder();
            for (T enumConstant : enumClass.getEnumConstants()) {
                if (validOptions.length() > 0) {
                    validOptions.append(", ");
                }
                validOptions.append(enumConstant.name());
            }

            throw new ArgumentParseException(
                    "Invalid " + enumClass.getSimpleName() + " '" + input + "'! Valid options: " + validOptions,
                    name, input
            );
        }
    }

    @Override
    public List<String> getCompletions(CommandSender sender) {
        List<String> completions = new ArrayList<>();
        for (T enumConstant : enumClass.getEnumConstants()) {
            completions.add(enumConstant.name());
        }
        return completions;
    }

    /**
     * Gets the enum class this argument type parses.
     *
     * @return The enum class
     */
    public Class<T> getEnumClass() {
        return enumClass;
    }
}
