package dev.aurora.transaction;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Manages command transactions across all commands.
 * Tracks active transactions and provides transaction context.
 */
public class TransactionManager {
    private final Map<String, CommandTransaction> activeTransactions;
    private final ThreadLocal<CommandTransaction> currentTransaction;
    private final Logger logger;

    public TransactionManager() {
        this.activeTransactions = new ConcurrentHashMap<>();
        this.currentTransaction = new ThreadLocal<>();
        this.logger = Logger.getLogger("AuroraCommands");
    }

    /**
     * Begin a new transaction for the current thread.
     *
     * @param commandName The command name
     * @return The new transaction
     */
    public CommandTransaction beginTransaction(String commandName) {
        String transactionId = commandName + "-" + UUID.randomUUID().toString().substring(0, 8);
        CommandTransaction transaction = new CommandTransaction(transactionId);

        currentTransaction.set(transaction);
        activeTransactions.put(transactionId, transaction);

        logger.fine("Started transaction: " + transactionId);
        return transaction;
    }

    /**
     * Get the current transaction for this thread.
     *
     * @return The current transaction, or null if none
     */
    public CommandTransaction getCurrentTransaction() {
        return currentTransaction.get();
    }

    /**
     * Check if there is an active transaction for this thread.
     *
     * @return True if transaction is active
     */
    public boolean hasActiveTransaction() {
        return currentTransaction.get() != null;
    }

    /**
     * Commit the current transaction and clean up.
     *
     * @return True if committed successfully
     */
    public boolean commitCurrentTransaction() {
        CommandTransaction transaction = currentTransaction.get();
        if (transaction == null) {
            return false;
        }

        try {
            transaction.commit();
            return true;
        } finally {
            cleanup(transaction);
        }
    }

    /**
     * Rollback the current transaction and clean up.
     *
     * @return True if rolled back successfully
     */
    public boolean rollbackCurrentTransaction() {
        CommandTransaction transaction = currentTransaction.get();
        if (transaction == null) {
            return false;
        }

        try {
            transaction.rollback();
            return true;
        } finally {
            cleanup(transaction);
        }
    }

    /**
     * Clean up transaction resources.
     *
     * @param transaction The transaction to clean up
     */
    private void cleanup(CommandTransaction transaction) {
        currentTransaction.remove();
        activeTransactions.remove(transaction.getTransactionId());
        logger.fine("Cleaned up transaction: " + transaction.getTransactionId());
    }

    /**
     * Record an action in the current transaction.
     *
     * @param action The action to record
     */
    public void recordAction(Reversible action) {
        CommandTransaction transaction = currentTransaction.get();
        if (transaction != null) {
            transaction.record(action);
        } else {
            logger.warning("Attempted to record action without active transaction: " +
                         action.getDescription());
        }
    }

    /**
     * Get the number of active transactions.
     *
     * @return Active transaction count
     */
    public int getActiveTransactionCount() {
        return activeTransactions.size();
    }

    /**
     * Get all active transaction IDs.
     *
     * @return Set of transaction IDs
     */
    public java.util.Set<String> getActiveTransactionIds() {
        return new java.util.HashSet<>(activeTransactions.keySet());
    }
}
