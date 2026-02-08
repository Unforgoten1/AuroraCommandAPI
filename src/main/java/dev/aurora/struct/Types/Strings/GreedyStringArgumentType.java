package dev.aurora.struct.Types.Strings;

import dev.aurora.exception.ArgumentParseException;
import dev.aurora.struct.ArgumentType;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

/**
 * ArgumentType that consumes all remaining arguments as a single string.
 * Useful for messages, reasons, descriptions, or any text input that may contain spaces.
 *
 * Example: "/message player1 Hello this is a long message"
 * - args[0] = "player1"
 * - args[1...] = "Hello this is a long message" (parsed by GreedyStringArgumentType)
 *
 * Note: This should typically be the last argument in a command since it consumes
 * all remaining input.
 */
public class GreedyStringArgumentType implements ArgumentType<String> {
    private final boolean allowEmpty;

    /**
     * Creates a GreedyStringArgumentType that requires at least one word.
     */
    public GreedyStringArgumentType() {
        this(false);
    }

    /**
     * Creates a GreedyStringArgumentType with configurable empty string handling.
     *
     * @param allowEmpty Whether to allow empty strings (no remaining arguments)
     */
    public GreedyStringArgumentType(boolean allowEmpty) {
        this.allowEmpty = allowEmpty;
    }

    @Override
    public String getName() {
        return "greedyString";
    }

    @Override
    public String parse(CommandSender sender, String input) throws ArgumentParseException {
        // Note: For greedy strings, we need special handling in AuroraCommand.execute()
        // to pass all remaining args as a single string. This parse method handles
        // the case where it's already been combined.

        if (input == null || input.trim().isEmpty()) {
            if (allowEmpty) {
                return "";
            }
            throw new ArgumentParseException("Text input cannot be empty!");
        }

        return input;
    }

    @Override
    public List<String> getCompletions(CommandSender sender) {
        // No tab completions for free-form text
        return new ArrayList<>();
    }

    /**
     * Checks if this argument type allows empty strings.
     *
     * @return True if empty strings are allowed
     */
    public boolean isAllowEmpty() {
        return allowEmpty;
    }
}
