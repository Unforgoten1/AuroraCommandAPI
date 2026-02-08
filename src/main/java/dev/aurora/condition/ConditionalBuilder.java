package dev.aurora.condition;

import dev.aurora.condition.builtin.TimeCondition;
import dev.aurora.condition.builtin.WeatherCondition;
import dev.aurora.condition.builtin.WorldCondition;
import dev.aurora.struct.CommandContext;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;

/**
 * Fluent builder for creating complex execution conditions.
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * command.addCondition(ConditionalBuilder.create()
 *     .time(12000, 24000)  // Night only
 *     .and()
 *     .world("world_nether")
 *     .failureMessage("§cOnly works at night in the Nether!")
 *     .build());
 * }</pre>
 */
public class ConditionalBuilder {
    private final List<ExecutionCondition> conditions;
    private LogicalOperator operator = LogicalOperator.AND;
    private String failureMessage = null;

    private enum LogicalOperator {
        AND, OR
    }

    private ConditionalBuilder() {
        this.conditions = new ArrayList<>();
    }

    /**
     * Create a new conditional builder.
     *
     * @return New builder
     */
    public static ConditionalBuilder create() {
        return new ConditionalBuilder();
    }

    /**
     * Add a time-based condition (Minecraft time ticks).
     *
     * @param minTime Minimum time (0-24000)
     * @param maxTime Maximum time (0-24000)
     * @return This builder
     */
    public ConditionalBuilder time(long minTime, long maxTime) {
        conditions.add(new TimeCondition(minTime, maxTime));
        return this;
    }

    /**
     * Add a world-based condition.
     *
     * @param worldName World name
     * @return This builder
     */
    public ConditionalBuilder world(String worldName) {
        conditions.add(new WorldCondition(worldName));
        return this;
    }

    /**
     * Add a weather-based condition.
     *
     * @param clearWeather True to require clear weather, false to require rain/storm
     * @return This builder
     */
    public ConditionalBuilder weather(boolean clearWeather) {
        conditions.add(new WeatherCondition(clearWeather));
        return this;
    }

    /**
     * Add a custom condition using a predicate.
     *
     * @param predicate The condition predicate
     * @param name Condition name
     * @param failureMsg Failure message
     * @return This builder
     */
    public ConditionalBuilder custom(BiPredicate<CommandSender, CommandContext> predicate,
                                    String name, String failureMsg) {
        conditions.add(new ExecutionCondition() {
            @Override
            public boolean test(CommandSender sender, CommandContext context) {
                return predicate.test(sender, context);
            }

            @Override
            public String getFailureMessage() {
                return failureMsg;
            }

            @Override
            public String getName() {
                return name;
            }
        });
        return this;
    }

    /**
     * Combine conditions with AND logic (all must be true).
     *
     * @return This builder
     */
    public ConditionalBuilder and() {
        this.operator = LogicalOperator.AND;
        return this;
    }

    /**
     * Combine conditions with OR logic (at least one must be true).
     *
     * @return This builder
     */
    public ConditionalBuilder or() {
        this.operator = LogicalOperator.OR;
        return this;
    }

    /**
     * Set a custom failure message for when conditions are not met.
     *
     * @param message Failure message
     * @return This builder
     */
    public ConditionalBuilder failureMessage(String message) {
        this.failureMessage = message;
        return this;
    }

    /**
     * Build the combined condition.
     *
     * @return ExecutionCondition combining all added conditions
     */
    public ExecutionCondition build() {
        final List<ExecutionCondition> finalConditions = new ArrayList<>(conditions);
        final LogicalOperator finalOperator = operator;
        final String finalMessage = failureMessage;

        return new ExecutionCondition() {
            @Override
            public boolean test(CommandSender sender, CommandContext context) {
                if (finalConditions.isEmpty()) {
                    return true;
                }

                if (finalOperator == LogicalOperator.AND) {
                    // All conditions must pass
                    for (ExecutionCondition condition : finalConditions) {
                        if (!condition.test(sender, context)) {
                            return false;
                        }
                    }
                    return true;
                } else {
                    // At least one condition must pass
                    for (ExecutionCondition condition : finalConditions) {
                        if (condition.test(sender, context)) {
                            return true;
                        }
                    }
                    return false;
                }
            }

            @Override
            public String getFailureMessage() {
                if (finalMessage != null) {
                    return finalMessage;
                }

                // Generate message from individual conditions
                if (finalConditions.isEmpty()) {
                    return "§cCommand execution conditions not met!";
                }

                if (finalConditions.size() == 1) {
                    return finalConditions.get(0).getFailureMessage();
                }

                return "§cCommand execution conditions not met! " +
                       "(" + finalConditions.size() + " conditions checked)";
            }

            @Override
            public String getName() {
                return "Combined Condition (" + finalConditions.size() + " checks)";
            }
        };
    }
}
