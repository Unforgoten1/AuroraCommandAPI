package dev.aurora.transaction.builtin;

import dev.aurora.transaction.Reversible;

/**
 * A simple reversible action that executes a runnable on reversal.
 * Useful for quick reversible actions without creating custom classes.
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * // Save original value
 * int originalHealth = player.getHealth();
 *
 * // Make change
 * player.setHealth(20);
 *
 * // Record reversal
 * transactionManager.recordAction(new SimpleReversible(
 *     "Set player health",
 *     () -> player.setHealth(originalHealth)
 * ));
 * }</pre>
 */
public class SimpleReversible implements Reversible {
    private final String description;
    private final Runnable reversalAction;

    /**
     * Create a simple reversible action.
     *
     * @param description Action description
     * @param reversalAction The action to execute on reversal
     */
    public SimpleReversible(String description, Runnable reversalAction) {
        this.description = description;
        this.reversalAction = reversalAction;
    }

    @Override
    public void reverse() throws Exception {
        reversalAction.run();
    }

    @Override
    public String getDescription() {
        return description;
    }

    /**
     * Create a reversible action with automatic description.
     *
     * @param reversalAction The reversal action
     * @return New reversible
     */
    public static SimpleReversible create(Runnable reversalAction) {
        return new SimpleReversible("Custom Action", reversalAction);
    }
}
