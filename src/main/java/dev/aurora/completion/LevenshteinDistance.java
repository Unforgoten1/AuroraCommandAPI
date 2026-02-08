package dev.aurora.completion;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Utility class for calculating string similarity using Levenshtein distance.
 * Used for fuzzy matching in tab completions and command suggestions.
 */
public class LevenshteinDistance {

    /**
     * Calculate the Levenshtein distance between two strings.
     * Lower distance = more similar strings.
     *
     * @param s1 First string
     * @param s2 Second string
     * @return The edit distance
     */
    public static int calculate(String s1, String s2) {
        if (s1 == null || s2 == null) {
            return Integer.MAX_VALUE;
        }

        String a = s1.toLowerCase();
        String b = s2.toLowerCase();

        int[][] dp = new int[a.length() + 1][b.length() + 1];

        for (int i = 0; i <= a.length(); i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= b.length(); j++) {
            dp[0][j] = j;
        }

        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                int cost = (a.charAt(i - 1) == b.charAt(j - 1)) ? 0 : 1;
                dp[i][j] = Math.min(
                    Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                    dp[i - 1][j - 1] + cost
                );
            }
        }

        return dp[a.length()][b.length()];
    }

    /**
     * Calculate similarity ratio between two strings (0.0 to 1.0).
     * 1.0 = identical, 0.0 = completely different.
     *
     * @param s1 First string
     * @param s2 Second string
     * @return Similarity ratio
     */
    public static double similarity(String s1, String s2) {
        if (s1 == null || s2 == null) {
            return 0.0;
        }
        if (s1.equalsIgnoreCase(s2)) {
            return 1.0;
        }

        int maxLen = Math.max(s1.length(), s2.length());
        if (maxLen == 0) {
            return 1.0;
        }

        int distance = calculate(s1, s2);
        return 1.0 - ((double) distance / maxLen);
    }

    /**
     * Find the best matching strings from a list.
     *
     * @param input The input to match against
     * @param candidates The list of candidate strings
     * @param maxResults Maximum number of results to return
     * @param threshold Minimum similarity threshold (0.0 to 1.0)
     * @return List of best matches, sorted by similarity
     */
    public static List<String> findBestMatches(String input, List<String> candidates,
                                               int maxResults, double threshold) {
        if (input == null || candidates == null || candidates.isEmpty()) {
            return Collections.emptyList();
        }

        return candidates.stream()
            .map(candidate -> new Match(candidate, similarity(input, candidate)))
            .filter(match -> match.similarity >= threshold)
            .sorted((a, b) -> Double.compare(b.similarity, a.similarity))
            .limit(maxResults)
            .map(match -> match.string)
            .collect(Collectors.toList());
    }

    /**
     * Find the single best match from a list.
     *
     * @param input The input to match against
     * @param candidates The list of candidate strings
     * @param threshold Minimum similarity threshold
     * @return The best match, or null if none meet the threshold
     */
    public static String findBestMatch(String input, List<String> candidates, double threshold) {
        List<String> matches = findBestMatches(input, candidates, 1, threshold);
        return matches.isEmpty() ? null : matches.get(0);
    }

    /**
     * Helper class to store a string with its similarity score.
     */
    private static class Match {
        final String string;
        final double similarity;

        Match(String string, double similarity) {
            this.string = string;
            this.similarity = similarity;
        }
    }
}
