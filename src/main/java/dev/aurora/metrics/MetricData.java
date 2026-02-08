package dev.aurora.metrics;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Stores execution metrics for a single command.
 * Thread-safe using atomic operations.
 */
public class MetricData {
    private final String commandName;
    private final AtomicInteger totalExecutions;
    private final AtomicInteger successfulExecutions;
    private final AtomicInteger failedExecutions;
    private final AtomicLong totalExecutionTime;
    private final AtomicLong minExecutionTime;
    private final AtomicLong maxExecutionTime;
    private final long createdAt;
    private volatile long lastExecutedAt;

    public MetricData(String commandName) {
        this.commandName = commandName;
        this.totalExecutions = new AtomicInteger(0);
        this.successfulExecutions = new AtomicInteger(0);
        this.failedExecutions = new AtomicInteger(0);
        this.totalExecutionTime = new AtomicLong(0);
        this.minExecutionTime = new AtomicLong(Long.MAX_VALUE);
        this.maxExecutionTime = new AtomicLong(0);
        this.createdAt = System.currentTimeMillis();
        this.lastExecutedAt = 0;
    }

    /**
     * Record a command execution.
     *
     * @param executionTimeMs Execution time in milliseconds
     * @param success Whether the execution was successful
     */
    public void recordExecution(long executionTimeMs, boolean success) {
        totalExecutions.incrementAndGet();
        if (success) {
            successfulExecutions.incrementAndGet();
        } else {
            failedExecutions.incrementAndGet();
        }

        totalExecutionTime.addAndGet(executionTimeMs);
        lastExecutedAt = System.currentTimeMillis();

        // Update min/max execution times
        updateMin(executionTimeMs);
        updateMax(executionTimeMs);
    }

    private void updateMin(long time) {
        long current;
        do {
            current = minExecutionTime.get();
            if (time >= current) {
                break;
            }
        } while (!minExecutionTime.compareAndSet(current, time));
    }

    private void updateMax(long time) {
        long current;
        do {
            current = maxExecutionTime.get();
            if (time <= current) {
                break;
            }
        } while (!maxExecutionTime.compareAndSet(current, time));
    }

    // Getters

    public String getCommandName() {
        return commandName;
    }

    public int getTotalExecutions() {
        return totalExecutions.get();
    }

    public int getSuccessfulExecutions() {
        return successfulExecutions.get();
    }

    public int getFailedExecutions() {
        return failedExecutions.get();
    }

    public double getSuccessRate() {
        int total = getTotalExecutions();
        if (total == 0) {
            return 0.0;
        }
        return (double) getSuccessfulExecutions() / total * 100.0;
    }

    public double getErrorRate() {
        return 100.0 - getSuccessRate();
    }

    public long getTotalExecutionTime() {
        return totalExecutionTime.get();
    }

    public double getAverageExecutionTime() {
        int total = getTotalExecutions();
        if (total == 0) {
            return 0.0;
        }
        return (double) getTotalExecutionTime() / total;
    }

    public long getMinExecutionTime() {
        long min = minExecutionTime.get();
        return min == Long.MAX_VALUE ? 0 : min;
    }

    public long getMaxExecutionTime() {
        return maxExecutionTime.get();
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getLastExecutedAt() {
        return lastExecutedAt;
    }

    /**
     * Get a formatted summary of the metrics.
     *
     * @return Formatted string with all metrics
     */
    public String getSummary() {
        return String.format(
            "§6Command: §e%s\n" +
            "§6Total Executions: §e%d\n" +
            "§6Success Rate: §e%.2f%% §7(%d successful, %d failed)\n" +
            "§6Avg Execution Time: §e%.2fms\n" +
            "§6Min/Max Time: §e%dms / %dms",
            commandName,
            getTotalExecutions(),
            getSuccessRate(),
            getSuccessfulExecutions(),
            getFailedExecutions(),
            getAverageExecutionTime(),
            getMinExecutionTime(),
            getMaxExecutionTime()
        );
    }

    @Override
    public String toString() {
        return "MetricData{" +
               "command='" + commandName + '\'' +
               ", executions=" + getTotalExecutions() +
               ", success=" + getSuccessfulExecutions() +
               ", failed=" + getFailedExecutions() +
               ", avgTime=" + String.format("%.2f", getAverageExecutionTime()) +
               "ms}";
    }
}
