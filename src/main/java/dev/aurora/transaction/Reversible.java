package dev.aurora.transaction;

/**
 * Marker interface for actions that can be reversed (undone).
 * Reversible actions can be tracked in transactions and rolled back on error.
 */
public interface Reversible {

    /**
     * Reverse (undo) this action.
     * Called when a transaction is rolled back.
     *
     * @throws Exception if the reversal fails
     */
    void reverse() throws Exception;

    /**
     * Get a description of this action.
     *
     * @return Action description
     */
    default String getDescription() {
        return "Reversible Action";
    }
}
