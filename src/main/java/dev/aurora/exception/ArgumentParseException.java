package dev.aurora.exception;

/**
 * Exception thrown when command argument parsing fails.
 * Includes contextual information about which argument failed and suggested corrections.
 */
public class ArgumentParseException extends Exception {
    private String argumentName;
    private String inputValue;
    private String suggestion;

    /**
     * Creates a new ArgumentParseException with just a message.
     *
     * @param message The error message
     */
    public ArgumentParseException(String message) {
        super(message);
    }

    /**
     * Creates a new ArgumentParseException with context about which argument failed.
     *
     * @param message      The error message
     * @param argumentName The name of the argument that failed parsing
     * @param inputValue   The input value that failed to parse
     */
    public ArgumentParseException(String message, String argumentName, String inputValue) {
        super(message);
        this.argumentName = argumentName;
        this.inputValue = inputValue;
    }

    /**
     * Creates a new ArgumentParseException with a suggestion for correction.
     *
     * @param message      The error message
     * @param argumentName The name of the argument that failed parsing
     * @param inputValue   The input value that failed to parse
     * @param suggestion   A suggested correction (e.g., "Did you mean 'Notch'?")
     */
    public ArgumentParseException(String message, String argumentName, String inputValue, String suggestion) {
        super(message);
        this.argumentName = argumentName;
        this.inputValue = inputValue;
        this.suggestion = suggestion;
    }

    /**
     * Gets the name of the argument that failed parsing.
     *
     * @return The argument name, or null if not set
     */
    public String getArgumentName() {
        return argumentName;
    }

    /**
     * Gets the input value that failed to parse.
     *
     * @return The input value, or null if not set
     */
    public String getInputValue() {
        return inputValue;
    }

    /**
     * Gets the suggested correction.
     *
     * @return The suggestion, or null if not set
     */
    public String getSuggestion() {
        return suggestion;
    }

    /**
     * Gets a detailed error message including context.
     *
     * @return A formatted error message with all available context
     */
    public String getDetailedMessage() {
        StringBuilder builder = new StringBuilder(getMessage());

        if (argumentName != null) {
            builder.append(" [Argument: ").append(argumentName).append("]");
        }

        if (inputValue != null) {
            builder.append(" [Input: '").append(inputValue).append("']");
        }

        if (suggestion != null) {
            builder.append(" - ").append(suggestion);
        }

        return builder.toString();
    }
}
