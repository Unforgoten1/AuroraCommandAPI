package dev.aurora.flags;

import dev.aurora.struct.ArgumentType;
import dev.aurora.struct.Types.Boolean.BooleanArgumentType;

/**
 * Fluent builder for creating command flags.
 *
 * Example usage:
 * <pre>
 * FlagBuilder.create("verbose")
 *     .shortForm('v')
 *     .longForm("verbose")
 *     .asBoolean()
 * </pre>
 */
public class FlagBuilder {
    private final String name;
    private Character shortForm;
    private String longForm;
    private ArgumentType<?> type;
    private Object defaultValue;
    private boolean isBoolean;
    private boolean isRequired;

    private FlagBuilder(String name) {
        this.name = name;
        this.isBoolean = false;
        this.isRequired = false;
    }

    /**
     * Create a new flag builder.
     *
     * @param name The flag name (used in context)
     * @return A new FlagBuilder instance
     */
    public static FlagBuilder create(String name) {
        return new FlagBuilder(name);
    }

    /**
     * Set the short form of the flag (e.g., 'v' for -v).
     *
     * @param shortForm The short form character
     * @return This builder
     */
    public FlagBuilder shortForm(char shortForm) {
        this.shortForm = shortForm;
        if (this.longForm == null) {
            this.longForm = name;
        }
        return this;
    }

    /**
     * Set the long form of the flag (e.g., "verbose" for --verbose).
     *
     * @param longForm The long form string
     * @return This builder
     */
    public FlagBuilder longForm(String longForm) {
        this.longForm = longForm;
        return this;
    }

    /**
     * Set the argument type for this flag's value.
     *
     * @param type The argument type
     * @return This builder
     */
    public FlagBuilder withType(ArgumentType<?> type) {
        this.type = type;
        this.isBoolean = false;
        return this;
    }

    /**
     * Set the default value for this flag.
     *
     * @param defaultValue The default value
     * @return This builder
     */
    public FlagBuilder withDefault(Object defaultValue) {
        this.defaultValue = defaultValue;
        return this;
    }

    /**
     * Mark this flag as a boolean flag (no value required).
     * Boolean flags are true when present, false when absent.
     *
     * @return This builder
     */
    public FlagBuilder asBoolean() {
        this.isBoolean = true;
        this.type = new BooleanArgumentType();
        if (this.defaultValue == null) {
            this.defaultValue = false;
        }
        return this;
    }

    /**
     * Mark this flag as required.
     *
     * @return This builder
     */
    public FlagBuilder required() {
        this.isRequired = true;
        return this;
    }

    /**
     * Build the flag.
     *
     * @return The constructed Flag
     * @throws IllegalStateException if flag configuration is invalid
     */
    public Flag build() {
        // Validation
        if (shortForm == null && longForm == null) {
            longForm = name; // Default to name as long form
        }

        if (!isBoolean && type == null) {
            throw new IllegalStateException("Flag '" + name + "' must have a type or be marked as boolean");
        }

        return new Flag(name, shortForm, longForm, type, defaultValue, isBoolean, isRequired);
    }
}
