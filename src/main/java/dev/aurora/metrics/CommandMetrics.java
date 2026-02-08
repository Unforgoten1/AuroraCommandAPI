package dev.aurora.metrics;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Central manager for command execution metrics.
 * Tracks statistics for all registered commands.
 */
public class CommandMetrics {
    private final Map<String, MetricData> metrics;

    public CommandMetrics() {
        this.metrics = new ConcurrentHashMap<>();
    }

    /**
     * Get or create metric data for a command.
     *
     * @param commandName Command name
     * @return Metric data for the command
     */
    public MetricData getMetrics(String commandName) {
        return metrics.computeIfAbsent(commandName.toLowerCase(), MetricData::new);
    }

    /**
     * Record a command execution.
     *
     * @param commandName Command name
     * @param executionTimeMs Execution time in milliseconds
     * @param success Whether execution was successful
     */
    public void recordExecution(String commandName, long executionTimeMs, boolean success) {
        getMetrics(commandName).recordExecution(executionTimeMs, success);
    }

    /**
     * Get all tracked command names.
     *
     * @return Set of command names
     */
    public Set<String> getTrackedCommands() {
        return new HashSet<>(metrics.keySet());
    }

    /**
     * Get metrics for all commands.
     *
     * @return Map of command names to metric data
     */
    public Map<String, MetricData> getAllMetrics() {
        return new HashMap<>(metrics);
    }

    /**
     * Get the top N most executed commands.
     *
     * @param limit Number of commands to return
     * @return List of metric data sorted by execution count
     */
    public List<MetricData> getTopExecuted(int limit) {
        return metrics.values().stream()
                .sorted((a, b) -> Integer.compare(b.getTotalExecutions(), a.getTotalExecutions()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Get the slowest commands.
     *
     * @param limit Number of commands to return
     * @return List of metric data sorted by average execution time
     */
    public List<MetricData> getSlowest(int limit) {
        return metrics.values().stream()
                .sorted((a, b) -> Double.compare(b.getAverageExecutionTime(), a.getAverageExecutionTime()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Get commands with the highest error rate.
     *
     * @param limit Number of commands to return
     * @return List of metric data sorted by error rate
     */
    public List<MetricData> getHighestErrorRate(int limit) {
        return metrics.values().stream()
                .filter(m -> m.getTotalExecutions() > 0)
                .sorted((a, b) -> Double.compare(b.getErrorRate(), a.getErrorRate()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Clear all metrics.
     */
    public void clear() {
        metrics.clear();
    }

    /**
     * Clear metrics for a specific command.
     *
     * @param commandName Command name
     */
    public void clearCommand(String commandName) {
        metrics.remove(commandName.toLowerCase());
    }

    /**
     * Get total executions across all commands.
     *
     * @return Total execution count
     */
    public int getTotalExecutions() {
        return metrics.values().stream()
                .mapToInt(MetricData::getTotalExecutions)
                .sum();
    }

    /**
     * Get overall success rate across all commands.
     *
     * @return Overall success rate percentage
     */
    public double getOverallSuccessRate() {
        int totalExecutions = getTotalExecutions();
        if (totalExecutions == 0) {
            return 0.0;
        }

        int successfulExecutions = metrics.values().stream()
                .mapToInt(MetricData::getSuccessfulExecutions)
                .sum();

        return (double) successfulExecutions / totalExecutions * 100.0;
    }

    /**
     * Get a summary report of all metrics.
     *
     * @return Formatted summary string
     */
    public String getSummaryReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("§6§l=== Command Metrics Summary ===\n\n");
        sb.append(String.format("§6Total Commands Tracked: §e%d\n", metrics.size()));
        sb.append(String.format("§6Total Executions: §e%d\n", getTotalExecutions()));
        sb.append(String.format("§6Overall Success Rate: §e%.2f%%\n\n", getOverallSuccessRate()));

        sb.append("§6Top 5 Most Executed Commands:\n");
        List<MetricData> topExecuted = getTopExecuted(5);
        for (int i = 0; i < topExecuted.size(); i++) {
            MetricData data = topExecuted.get(i);
            sb.append(String.format("  §e%d. §f%s §7(%d executions)\n",
                    i + 1, data.getCommandName(), data.getTotalExecutions()));
        }

        sb.append("\n§6Top 5 Slowest Commands:\n");
        List<MetricData> slowest = getSlowest(5);
        for (int i = 0; i < slowest.size(); i++) {
            MetricData data = slowest.get(i);
            sb.append(String.format("  §e%d. §f%s §7(%.2fms avg)\n",
                    i + 1, data.getCommandName(), data.getAverageExecutionTime()));
        }

        return sb.toString();
    }
}
