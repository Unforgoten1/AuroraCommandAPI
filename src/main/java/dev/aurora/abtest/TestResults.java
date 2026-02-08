package dev.aurora.abtest;

import java.util.List;
import java.util.Map;

/**
 * Displays A/B test results and statistics.
 */
public class TestResults {

    /**
     * Generate a formatted results report for an A/B test.
     *
     * @param test The A/B test
     * @return Formatted results string
     */
    public static String generateReport(ABTest test) {
        StringBuilder sb = new StringBuilder();

        sb.append("§6§l=== A/B Test Results: ").append(test.getTestName()).append(" ===\n\n");

        List<Variant> variants = test.getVariants();
        Map<String, Integer> distribution = test.getDistribution();

        // Calculate totals
        int totalExecutions = variants.stream().mapToInt(Variant::getExecutionCount).sum();
        int totalPlayers = distribution.values().stream().mapToInt(Integer::intValue).sum();

        sb.append("§6Test Duration: §e").append(formatDuration(System.currentTimeMillis() - test.getCreatedAt())).append("\n");
        sb.append("§6Total Players: §e").append(totalPlayers).append("\n");
        sb.append("§6Total Executions: §e").append(totalExecutions).append("\n\n");

        // Show results for each variant
        for (Variant variant : variants) {
            int playerCount = distribution.getOrDefault(variant.getName(), 0);
            double playerPercentage = totalPlayers > 0 ? (double) playerCount / totalPlayers * 100.0 : 0.0;

            sb.append("§e§l").append(variant.getName()).append("§r\n");
            sb.append(String.format("  §6Players: §e%d §7(%.1f%%)\n", playerCount, playerPercentage));
            sb.append(String.format("  §6Executions: §e%d\n", variant.getExecutionCount()));
            sb.append(String.format("  §6Success Rate: §e%.2f%%\n", variant.getSuccessRate()));
            sb.append(String.format("  §6Avg Time: §e%.2fms\n", variant.getAverageExecutionTime()));
            sb.append("\n");
        }

        // Determine winner if there's clear data
        if (totalExecutions > 100) {
            Variant winner = determineWinner(variants);
            if (winner != null) {
                sb.append("§a§l✓ Recommended Winner: §e").append(winner.getName()).append("\n");
                sb.append("§7Based on success rate and execution count\n");
            }
        } else {
            sb.append("§7Insufficient data to determine winner (need >100 executions)\n");
        }

        return sb.toString();
    }

    /**
     * Determine the winning variant based on success rate.
     *
     * @param variants List of variants
     * @return Winning variant, or null if no clear winner
     */
    private static Variant determineWinner(List<Variant> variants) {
        Variant best = null;
        double bestScore = 0.0;

        for (Variant variant : variants) {
            if (variant.getExecutionCount() < 10) {
                continue; // Need minimum executions
            }

            // Score based on success rate and execution count
            double score = variant.getSuccessRate() * Math.log(variant.getExecutionCount() + 1);

            if (best == null || score > bestScore) {
                best = variant;
                bestScore = score;
            }
        }

        return best;
    }

    /**
     * Format a duration in milliseconds to a readable string.
     *
     * @param millis Duration in milliseconds
     * @return Formatted string
     */
    private static String formatDuration(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) {
            return days + " day" + (days > 1 ? "s" : "");
        } else if (hours > 0) {
            return hours + " hour" + (hours > 1 ? "s" : "");
        } else if (minutes > 0) {
            return minutes + " minute" + (minutes > 1 ? "s" : "");
        } else {
            return seconds + " second" + (seconds != 1 ? "s" : "");
        }
    }

    /**
     * Generate a brief summary of test results.
     *
     * @param test The A/B test
     * @return Brief summary string
     */
    public static String generateSummary(ABTest test) {
        List<Variant> variants = test.getVariants();
        int totalExecutions = variants.stream().mapToInt(Variant::getExecutionCount).sum();

        if (totalExecutions == 0) {
            return "§eTest: " + test.getTestName() + " §7(No data yet)";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("§eTest: ").append(test.getTestName()).append(" §7(");

        for (int i = 0; i < variants.size(); i++) {
            Variant v = variants.get(i);
            if (i > 0) sb.append(", ");
            sb.append(v.getName()).append(": ").append(v.getExecutionCount()).append(" runs");
        }

        sb.append(")");
        return sb.toString();
    }
}
