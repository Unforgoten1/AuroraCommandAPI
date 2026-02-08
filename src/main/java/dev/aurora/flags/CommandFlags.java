package dev.aurora.flags;

import dev.aurora.exception.ArgumentParseException;
import org.bukkit.command.CommandSender;

import java.util.*;

/**
 * Manages command flags and parses them from command arguments.
 */
public class CommandFlags {
    private final Map<String, Flag> flags;

    public CommandFlags() {
        this.flags = new HashMap<>();
    }

    /**
     * Add a flag to this command.
     *
     * @param flag The flag to add
     */
    public void addFlag(Flag flag) {
        flags.put(flag.getName(), flag);
    }

    /**
     * Check if this command has any flags defined.
     *
     * @return True if flags are defined
     */
    public boolean hasFlags() {
        return !flags.isEmpty();
    }

    /**
     * Get all flag completions for the given input.
     *
     * @param input The current input (e.g., "--", "-")
     * @return List of flag completion suggestions
     */
    public List<String> getFlagCompletions(String input) {
        List<String> completions = new ArrayList<>();

        if (input.startsWith("--")) {
            // Long form completions
            for (Flag flag : flags.values()) {
                if (flag.hasLongForm()) {
                    String completion = "--" + flag.getLongForm();
                    if (completion.startsWith(input)) {
                        completions.add(completion);
                    }
                }
            }
        } else if (input.startsWith("-") && input.length() < 3) {
            // Short form completions
            for (Flag flag : flags.values()) {
                if (flag.hasShortForm()) {
                    String completion = "-" + flag.getShortForm();
                    if (completion.startsWith(input)) {
                        completions.add(completion);
                    }
                }
            }
        }

        return completions;
    }

    /**
     * Parse flags from the command arguments.
     * Extracts flag values and returns remaining arguments.
     *
     * @param sender The command sender
     * @param args The command arguments
     * @return ParsedFlags containing flag values and remaining args
     * @throws ArgumentParseException if flag parsing fails
     */
    public ParsedFlags parse(CommandSender sender, String[] args) throws ArgumentParseException {
        Map<String, Object> flagValues = new HashMap<>();
        List<String> remainingArgs = new ArrayList<>();

        // Initialize defaults
        for (Flag flag : flags.values()) {
            if (flag.hasDefaultValue()) {
                flagValues.put(flag.getName(), flag.getDefaultValue());
            }
        }

        int i = 0;
        while (i < args.length) {
            String arg = args[i];

            // Check if this is a flag
            Flag matchedFlag = findMatchingFlag(arg);

            if (matchedFlag != null) {
                // Parse flag value
                Object value;

                if (matchedFlag.isBoolean()) {
                    // Boolean flag - presence = true
                    value = true;
                    i++;
                } else if (arg.contains("=")) {
                    // Long form with value: --flag=value
                    String valueStr = arg.substring(arg.indexOf('=') + 1);
                    value = matchedFlag.getType().parse(sender, valueStr);
                    i++;
                } else {
                    // Next argument is the value
                    if (i + 1 >= args.length) {
                        throw new ArgumentParseException("Flag " + matchedFlag + " requires a value");
                    }
                    String valueStr = args[i + 1];
                    value = matchedFlag.getType().parse(sender, valueStr);
                    i += 2;
                }

                flagValues.put(matchedFlag.getName(), value);
            } else {
                // Not a flag, add to remaining args
                remainingArgs.add(arg);
                i++;
            }
        }

        // Check for required flags
        for (Flag flag : flags.values()) {
            if (flag.isRequired() && !flagValues.containsKey(flag.getName())) {
                throw new ArgumentParseException("Required flag " + flag + " is missing");
            }
        }

        return new ParsedFlags(flagValues, remainingArgs.toArray(new String[0]));
    }

    /**
     * Parse flags partially (for tab completion).
     * More lenient than full parse.
     *
     * @param args The command arguments
     * @return Partially parsed flags
     */
    public ParsedFlags parsePartial(String[] args) {
        Map<String, Object> flagValues = new HashMap<>();
        List<String> remainingArgs = new ArrayList<>();

        // Initialize defaults
        for (Flag flag : flags.values()) {
            if (flag.hasDefaultValue()) {
                flagValues.put(flag.getName(), flag.getDefaultValue());
            }
        }

        int i = 0;
        while (i < args.length) {
            String arg = args[i];
            Flag matchedFlag = findMatchingFlag(arg);

            if (matchedFlag != null) {
                if (matchedFlag.isBoolean()) {
                    flagValues.put(matchedFlag.getName(), true);
                    i++;
                } else if (arg.contains("=")) {
                    i++;
                } else if (i + 1 < args.length) {
                    i += 2;
                } else {
                    i++;
                }
            } else {
                remainingArgs.add(arg);
                i++;
            }
        }

        return new ParsedFlags(flagValues, remainingArgs.toArray(new String[0]));
    }

    /**
     * Find a flag that matches the given input.
     *
     * @param input The input string
     * @return The matching flag, or null
     */
    private Flag findMatchingFlag(String input) {
        for (Flag flag : flags.values()) {
            if (flag.matches(input)) {
                return flag;
            }
        }
        return null;
    }

    /**
     * Parsed flags result containing flag values and remaining arguments.
     */
    public static class ParsedFlags {
        private final Map<String, Object> flagValues;
        private final String[] remainingArgs;

        public ParsedFlags(Map<String, Object> flagValues, String[] remainingArgs) {
            this.flagValues = flagValues;
            this.remainingArgs = remainingArgs;
        }

        /**
         * Get a flag value.
         *
         * @param flagName The flag name
         * @param <T> The value type
         * @return The flag value, or null if not set
         */
        @SuppressWarnings("unchecked")
        public <T> T get(String flagName) {
            return (T) flagValues.get(flagName);
        }

        /**
         * Check if a flag is set (present in the command).
         *
         * @param flagName The flag name
         * @return True if the flag was set
         */
        public boolean has(String flagName) {
            return flagValues.containsKey(flagName);
        }

        /**
         * Get all flag names that were set.
         *
         * @return Set of flag names
         */
        public Set<String> getFlagNames() {
            return new HashSet<>(flagValues.keySet());
        }

        /**
         * Get the remaining arguments (non-flag arguments).
         *
         * @return Array of remaining arguments
         */
        public String[] remainingArgs() {
            return remainingArgs;
        }

        /**
         * Check if this result has any flags set.
         *
         * @return True if flags were parsed
         */
        public boolean hasPipelineInput() {
            return false; // Placeholder for pipeline support
        }
    }
}
