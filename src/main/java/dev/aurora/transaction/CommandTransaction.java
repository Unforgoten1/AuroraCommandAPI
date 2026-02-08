package dev.aurora.transaction;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Represents a transaction that tracks reversible actions during command execution.
 * If the command fails, all actions are automatically rolled back.
 */
public class CommandTransaction {
    private final String transactionId;
    private final List<Reversible> actions;
    private final long startTime;
    private boolean committed;
    private boolean rolledBack;
    private final Logger logger;

    /**
     * Create a new transaction.
     *
     * @param transactionId Unique transaction ID
     */
    public CommandTransaction(String transactionId) {
        this.transactionId = transactionId;
        this.actions = new ArrayList<>();
        this.startTime = System.currentTimeMillis();
        this.committed = false;
        this.rolledBack = false;
        this.logger = Logger.getLogger("AuroraCommands");
    }

    /**
     * Record a reversible action in this transaction.
     *
     * @param action The action to record
     */
    public void record(Reversible action) {
        if (committed) {
            throw new IllegalStateException("Cannot record action in committed transaction");
        }
        if (rolledBack) {
            throw new IllegalStateException("Cannot record action in rolled back transaction");
        }

        actions.add(action);
        logger.fine("Recorded action in transaction " + transactionId + ": " + action.getDescription());
    }

    /**
     * Commit the transaction, making all changes permanent.
     * After commit, actions cannot be rolled back.
     */
    public void commit() {
        if (committed) {
            return; // Already committed
        }
        if (rolledBack) {
            throw new IllegalStateException("Cannot commit a rolled back transaction");
        }

        committed = true;
        long duration = System.currentTimeMillis() - startTime;
        logger.info("Transaction " + transactionId + " committed with " + actions.size() +
                   " actions in " + duration + "ms");
    }

    /**
     * Rollback the transaction, reversing all recorded actions.
     * Actions are reversed in reverse order (last to first).
     */
    public void rollback() {
        if (committed) {
            throw new IllegalStateException("Cannot rollback a committed transaction");
        }
        if (rolledBack) {
            return; // Already rolled back
        }

        logger.info("Rolling back transaction " + transactionId + " with " + actions.size() + " actions");

        int successfulReversals = 0;
        int failedReversals = 0;

        // Reverse actions in reverse order
        for (int i = actions.size() - 1; i >= 0; i--) {
            Reversible action = actions.get(i);
            try {
                action.reverse();
                successfulReversals++;
                logger.fine("Reversed action: " + action.getDescription());
            } catch (Exception e) {
                failedReversals++;
                logger.severe("Failed to reverse action: " + action.getDescription() +
                            " - Error: " + e.getMessage());
                e.printStackTrace();
            }
        }

        rolledBack = true;
        long duration = System.currentTimeMillis() - startTime;

        logger.info("Transaction " + transactionId + " rolled back in " + duration + "ms" +
                   " (successful: " + successfulReversals + ", failed: " + failedReversals + ")");
    }

    /**
     * Get the number of actions recorded in this transaction.
     *
     * @return Action count
     */
    public int getActionCount() {
        return actions.size();
    }

    /**
     * Check if the transaction has been committed.
     *
     * @return True if committed
     */
    public boolean isCommitted() {
        return committed;
    }

    /**
     * Check if the transaction has been rolled back.
     *
     * @return True if rolled back
     */
    public boolean isRolledBack() {
        return rolledBack;
    }

    /**
     * Get the transaction ID.
     *
     * @return Transaction ID
     */
    public String getTransactionId() {
        return transactionId;
    }

    /**
     * Get the duration of this transaction in milliseconds.
     *
     * @return Duration in milliseconds
     */
    public long getDuration() {
        return System.currentTimeMillis() - startTime;
    }
}
