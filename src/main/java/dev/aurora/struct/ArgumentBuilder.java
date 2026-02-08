package dev.aurora.struct;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Fluent builder for creating advanced command arguments with validation,
 * defaults, conditional parsing, and dependencies.
 *
 * Example usage:
 * <pre>
 * ArgumentBuilder.create("amount", new IntegerArgumentType())
 *     .withDefault(1)
 *     .withValidator(val -> val > 0, "Amount must be positive")
 *     .withDescription("The amount to give")
 *     .build();
 * </pre>
 */
public class ArgumentBuilder<T> {
    private final String name;
    private final ArgumentType<T> type;
    private T defaultValue;
    private Predicate<T> validator;
    private String validationErrorMessage;
    private Predicate<CommandContext> condition;
    private Map<String, Object> requiredValues;
    private String description;

    private ArgumentBuilder(String name, ArgumentType<T> type) {
        this.name = name;
        this.type = type;
        this.requiredValues = new HashMap<>();
    }

    /**
     * Creates a new ArgumentBuilder for the specified argument.
     *
     * @param name The argument name
     * @param type The argument type
     * @param <T>  The value type
     * @return A new ArgumentBuilder
     */
    public static <T> ArgumentBuilder<T> create(String name, ArgumentType<T> type) {
        return new ArgumentBuilder<>(name, type);
    }

    /**
     * Sets a default value for this argument, making it optional.
     * If the argument is not provided, the default value will be used.
     *
     * @param defaultValue The default value
     * @return This ArgumentBuilder for chaining
     */
    public ArgumentBuilder<T> withDefault(T defaultValue) {
        this.defaultValue = defaultValue;
        return this;
    }

    /**
     * Adds a validator predicate that must return true for the parsed value.
     * If validation fails, the error message will be shown to the user.
     *
     * @param validator    The validation predicate
     * @param errorMessage The error message to show on validation failure
     * @return This ArgumentBuilder for chaining
     */
    public ArgumentBuilder<T> withValidator(Predicate<T> validator, String errorMessage) {
        this.validator = validator;
        this.validationErrorMessage = errorMessage;
        return this;
    }

    /**
     * Adds a condition that determines whether this argument should be parsed.
     * The condition receives the current CommandContext and returns true if
     * this argument should be parsed.
     *
     * Example: Only parse "duration" if "permanent" is false
     * <pre>
     * .withCondition(ctx -> !ctx.getArgument("permanent"))
     * </pre>
     *
     * @param condition The condition predicate
     * @return This ArgumentBuilder for chaining
     */
    public ArgumentBuilder<T> withCondition(Predicate<CommandContext> condition) {
        this.condition = condition;
        return this;
    }

    /**
     * Specifies that this argument requires another argument to have a specific value.
     * This is a shorthand for withCondition() for simple equality checks.
     *
     * Example: Only parse if "type" equals "custom"
     * <pre>
     * .requiresArgument("type", "custom")
     * </pre>
     *
     * @param argumentName  The name of the required argument
     * @param requiredValue The value it must equal
     * @return This ArgumentBuilder for chaining
     */
    public ArgumentBuilder<T> requiresArgument(String argumentName, Object requiredValue) {
        this.requiredValues.put(argumentName, requiredValue);
        return this;
    }

    /**
     * Sets a description for this argument.
     * Used in help generation and tab completion hints.
     *
     * @param description The argument description
     * @return This ArgumentBuilder for chaining
     */
    public ArgumentBuilder<T> withDescription(String description) {
        this.description = description;
        return this;
    }

    /**
     * Builds and returns the configured argument properties as a map.
     * This is used internally by AuroraCommand.
     *
     * @return A map containing all argument configuration
     */
    public Map<String, Object> build() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("name", name);
        properties.put("type", type);
        properties.put("defaultValue", defaultValue);
        properties.put("validator", validator);
        properties.put("validationErrorMessage", validationErrorMessage);
        properties.put("condition", condition);
        properties.put("requiredValues", requiredValues);
        properties.put("description", description);
        return properties;
    }

    // Getters for internal use
    public String getName() {
        return name;
    }

    public ArgumentType<T> getType() {
        return type;
    }

    public T getDefaultValue() {
        return defaultValue;
    }

    public Predicate<T> getValidator() {
        return validator;
    }

    public String getValidationErrorMessage() {
        return validationErrorMessage;
    }

    public Predicate<CommandContext> getCondition() {
        return condition;
    }

    public Map<String, Object> getRequiredValues() {
        return requiredValues;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Checks if this argument has a default value.
     *
     * @return True if a default value is set
     */
    public boolean hasDefault() {
        return defaultValue != null;
    }

    /**
     * Checks if this argument has a validator.
     *
     * @return True if a validator is set
     */
    public boolean hasValidator() {
        return validator != null;
    }

    /**
     * Checks if this argument has a condition.
     *
     * @return True if a condition is set
     */
    public boolean hasCondition() {
        return condition != null || !requiredValues.isEmpty();
    }

    /**
     * Evaluates all conditions for this argument.
     *
     * @param context The current command context
     * @return True if all conditions are met (argument should be parsed)
     */
    public boolean evaluateConditions(CommandContext context) {
        // Check custom condition predicate
        if (condition != null && !condition.test(context)) {
            return false;
        }

        // Check required argument values
        for (Map.Entry<String, Object> entry : requiredValues.entrySet()) {
            Object actualValue = context.getArgument(entry.getKey());
            Object requiredValue = entry.getValue();

            if (actualValue == null || !actualValue.equals(requiredValue)) {
                return false;
            }
        }

        return true;
    }
}
