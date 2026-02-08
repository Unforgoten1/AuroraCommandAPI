package dev.aurora.flags;

import dev.aurora.struct.ArgumentType;

/**
 * Represents a command flag/option.
 * Supports both short form (-f) and long form (--flag) syntax.
 */
public class Flag {
    private final String name;
    private final Character shortForm;
    private final String longForm;
    private final ArgumentType<?> type;
    private final Object defaultValue;
    private final boolean isBoolean;
    private final boolean isRequired;

    public Flag(String name, Character shortForm, String longForm, ArgumentType<?> type,
                Object defaultValue, boolean isBoolean, boolean isRequired) {
        this.name = name;
        this.shortForm = shortForm;
        this.longForm = longForm;
        this.type = type;
        this.defaultValue = defaultValue;
        this.isBoolean = isBoolean;
        this.isRequired = isRequired;
    }

    public String getName() {
        return name;
    }

    public Character getShortForm() {
        return shortForm;
    }

    public String getLongForm() {
        return longForm;
    }

    public ArgumentType<?> getType() {
        return type;
    }

    public Object getDefaultValue() {
        return defaultValue;
    }

    public boolean isBoolean() {
        return isBoolean;
    }

    public boolean isRequired() {
        return isRequired;
    }

    public boolean hasShortForm() {
        return shortForm != null;
    }

    public boolean hasLongForm() {
        return longForm != null;
    }

    public boolean hasDefaultValue() {
        return defaultValue != null;
    }

    /**
     * Check if this flag matches the given input string.
     *
     * @param input The input string (e.g., "-f", "--flag")
     * @return True if matches
     */
    public boolean matches(String input) {
        if (input == null) {
            return false;
        }

        // Short form: -f
        if (shortForm != null && input.equals("-" + shortForm)) {
            return true;
        }

        // Long form: --flag
        if (longForm != null && input.equals("--" + longForm)) {
            return true;
        }

        // Long form with value: --flag=value
        if (longForm != null && input.startsWith("--" + longForm + "=")) {
            return true;
        }

        return false;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (shortForm != null) {
            sb.append("-").append(shortForm);
        }
        if (shortForm != null && longForm != null) {
            sb.append(", ");
        }
        if (longForm != null) {
            sb.append("--").append(longForm);
        }
        return sb.toString();
    }
}
