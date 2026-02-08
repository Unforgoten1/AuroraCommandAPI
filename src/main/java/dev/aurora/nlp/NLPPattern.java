package dev.aurora.nlp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents a natural language pattern for command matching.
 * Patterns use placeholders like {player}, {amount}, {item} to extract entities.
 *
 * Example: "give {player} {amount} {item}" matches "give Player123 64 diamonds"
 */
public class NLPPattern {
    private final String pattern;
    private final List<String> placeholders;
    private final Pattern regex;
    private final String commandTemplate;

    /**
     * Create an NLP pattern.
     *
     * @param pattern The pattern string with placeholders
     * @param commandTemplate The command template to generate (e.g., "give {player} {item} {amount}")
     */
    public NLPPattern(String pattern, String commandTemplate) {
        this.pattern = pattern.toLowerCase();
        this.commandTemplate = commandTemplate;
        this.placeholders = new ArrayList<>();

        // Extract placeholders and build regex
        this.regex = buildRegex(pattern);
    }

    /**
     * Build a regex pattern from the NLP pattern string.
     *
     * @param patternStr The pattern string
     * @return Compiled regex pattern
     */
    private Pattern buildRegex(String patternStr) {
        StringBuilder regexBuilder = new StringBuilder("^");
        String[] tokens = patternStr.split("\\s+");

        for (String token : tokens) {
            if (token.startsWith("{") && token.endsWith("}")) {
                // This is a placeholder
                String placeholder = token.substring(1, token.length() - 1);
                placeholders.add(placeholder);

                // Match one or more words (greedy)
                regexBuilder.append("(.+?)");
            } else {
                // This is a literal word
                regexBuilder.append(Pattern.quote(token));
            }
            regexBuilder.append("\\s+");
        }

        // Remove trailing \s+
        if (regexBuilder.length() > 1) {
            regexBuilder.setLength(regexBuilder.length() - 3);
        }
        regexBuilder.append("$");

        return Pattern.compile(regexBuilder.toString(), Pattern.CASE_INSENSITIVE);
    }

    /**
     * Try to match input against this pattern.
     *
     * @param input The user input
     * @return Extracted entities if matched, or null if no match
     */
    public Map<String, String> match(String input) {
        Matcher matcher = regex.matcher(input.toLowerCase().trim());

        if (!matcher.matches()) {
            return null;
        }

        Map<String, String> entities = new HashMap<>();
        for (int i = 0; i < placeholders.size(); i++) {
            String placeholder = placeholders.get(i);
            String value = matcher.group(i + 1).trim();
            entities.put(placeholder, value);
        }

        return entities;
    }

    /**
     * Generate a command string from extracted entities.
     *
     * @param entities The extracted entities
     * @return Command string
     */
    public String generateCommand(Map<String, String> entities) {
        String command = commandTemplate;

        for (Map.Entry<String, String> entry : entities.entrySet()) {
            String placeholder = "{" + entry.getKey() + "}";
            command = command.replace(placeholder, entry.getValue());
        }

        return command;
    }

    /**
     * Get the pattern string.
     *
     * @return The pattern
     */
    public String getPattern() {
        return pattern;
    }

    /**
     * Get the command template.
     *
     * @return The command template
     */
    public String getCommandTemplate() {
        return commandTemplate;
    }

    /**
     * Get the placeholders in this pattern.
     *
     * @return List of placeholders
     */
    public List<String> getPlaceholders() {
        return new ArrayList<>(placeholders);
    }

    @Override
    public String toString() {
        return "NLPPattern{pattern='" + pattern + "', template='" + commandTemplate + "'}";
    }
}
