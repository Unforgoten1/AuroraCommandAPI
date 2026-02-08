package dev.aurora.abtest;

import dev.aurora.struct.CommandContext;
import org.bukkit.command.CommandSender;

import java.util.function.BiConsumer;

/**
 * Represents a single variant in an A/B test.
 * Each variant has a weight (for distribution) and an executor.
 */
public class Variant {
    private final String name;
    private final int weight;
    private BiConsumer<CommandSender, CommandContext> executor;
    private int executionCount;
    private int successCount;
    private long totalExecutionTime;

    /**
     * Create a new variant.
     *
     * @param name Variant name
     * @param weight Weight for distribution (higher = more likely)
     */
    public Variant(String name, int weight) {
        this.name = name;
        this.weight = Math.max(1, weight);
        this.executionCount = 0;
        this.successCount = 0;
        this.totalExecutionTime = 0;
    }

    /**
     * Create a variant with equal weight.
     *
     * @param name Variant name
     * @return New variant
     */
    public static Variant create(String name) {
        return new Variant(name, 50);
    }

    /**
     * Create a variant with specific weight.
     *
     * @param name Variant name
     * @param weight Weight percentage (0-100)
     * @return New variant
     */
    public static Variant create(String name, int weight) {
        return new Variant(name, weight);
    }

    /**
     * Set the executor for this variant.
     *
     * @param executor The executor function
     * @return This variant for chaining
     */
    public Variant withExecutor(BiConsumer<CommandSender, CommandContext> executor) {
        this.executor = executor;
        return this;
    }

    /**
     * Record an execution of this variant.
     *
     * @param executionTimeMs Execution time in milliseconds
     * @param success Whether execution was successful
     */
    public synchronized void recordExecution(long executionTimeMs, boolean success) {
        executionCount++;
        if (success) {
            successCount++;
        }
        totalExecutionTime += executionTimeMs;
    }

    // Getters

    public String getName() {
        return name;
    }

    public int getWeight() {
        return weight;
    }

    public BiConsumer<CommandSender, CommandContext> getExecutor() {
        return executor;
    }

    public int getExecutionCount() {
        return executionCount;
    }

    public int getSuccessCount() {
        return successCount;
    }

    public double getSuccessRate() {
        if (executionCount == 0) return 0.0;
        return (double) successCount / executionCount * 100.0;
    }

    public double getAverageExecutionTime() {
        if (executionCount == 0) return 0.0;
        return (double) totalExecutionTime / executionCount;
    }
}
