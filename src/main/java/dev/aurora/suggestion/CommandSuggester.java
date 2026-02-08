package dev.aurora.suggestion;

import dev.aurora.completion.LevenshteinDistance;

import java.util.ArrayList;
import java.util.List;

/**
 * Provides "Did you mean?" suggestions for mistyped commands.
 * Uses Levenshtein distance to find similar command names.
 */
public class CommandSuggester {
    private static final double DEFAULT_THRESHOLD = 0.5; // 50% similarity minimum
    private static final int MAX_SUGGESTIONS = 3;

    /**
     * Find the best command suggestion for a mistyped command.
     *
     * @param input The mistyped command
     * @param availableCommands List of valid command names
     * @return The best suggestion, or null if none found
     */
    public static String suggestCommand(String input, List<String> availableCommands) {
        return suggestCommand(input, availableCommands, DEFAULT_THRESHOLD);
    }

    /**
     * Find the best command suggestion for a mistyped command.
     *
     * @param input The mistyped command
     * @param availableCommands List of valid command names
     * @param threshold Minimum similarity threshold (0.0 to 1.0)
     * @return The best suggestion, or null if none found
     */
    public static String suggestCommand(String input, List<String> availableCommands, double threshold) {
        if (input == null || input.isEmpty() || availableCommands == null || availableCommands.isEmpty()) {
            return null;
        }

        String bestMatch = null;
        double bestSimilarity = threshold;

        for (String command : availableCommands) {
            double similarity = LevenshteinDistance.similarity(input, command);
            if (similarity > bestSimilarity) {
                bestSimilarity = similarity;
                bestMatch = command;
            }
        }

        return bestMatch;
    }

    /**
     * Find multiple command suggestions for a mistyped command.
     *
     * @param input The mistyped command
     * @param availableCommands List of valid command names
     * @return List of suggestions (up to MAX_SUGGESTIONS)
     */
    public static List<String> suggestCommands(String input, List<String> availableCommands) {
        return suggestCommands(input, availableCommands, DEFAULT_THRESHOLD, MAX_SUGGESTIONS);
    }

    /**
     * Find multiple command suggestions for a mistyped command.
     *
     * @param input The mistyped command
     * @param availableCommands List of valid command names
     * @param threshold Minimum similarity threshold (0.0 to 1.0)
     * @param maxSuggestions Maximum number of suggestions to return
     * @return List of suggestions
     */
    public static List<String> suggestCommands(String input, List<String> availableCommands,
                                              double threshold, int maxSuggestions) {
        if (input == null || input.isEmpty() || availableCommands == null || availableCommands.isEmpty()) {
            return new ArrayList<>();
        }

        return LevenshteinDistance.findBestMatches(input, availableCommands, maxSuggestions, threshold);
    }

    /**
     * Format a "did you mean?" message with a single suggestion.
     *
     * @param suggestion The suggested command
     * @return Formatted message
     */
    public static String formatSuggestion(String suggestion) {
        return "§cUnknown command. Did you mean: §e" + suggestion + "§c?";
    }

    /**
     * Format a "did you mean?" message with multiple suggestions.
     *
     * @param suggestions List of suggested commands
     * @return Formatted message
     */
    public static String formatSuggestions(List<String> suggestions) {
        if (suggestions == null || suggestions.isEmpty()) {
            return "§cUnknown command.";
        }

        if (suggestions.size() == 1) {
            return formatSuggestion(suggestions.get(0));
        }

        StringBuilder message = new StringBuilder("§cUnknown command. Did you mean: ");
        for (int i = 0; i < suggestions.size(); i++) {
            if (i > 0) {
                message.append("§c, ");
            }
            message.append("§e").append(suggestions.get(i));
        }
        message.append("§c?");
        return message.toString();
    }

    /**
     * Create a suggestion message for a mistyped command.
     *
     * @param input The mistyped command
     * @param availableCommands List of valid command names
     * @return Formatted suggestion message, or generic error if no suggestions
     */
    public static String createSuggestionMessage(String input, List<String> availableCommands) {
        List<String> suggestions = suggestCommands(input, availableCommands);
        return formatSuggestions(suggestions);
    }
}
