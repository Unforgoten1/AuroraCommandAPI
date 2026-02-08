package dev.aurora.abtest;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Manages all A/B tests for commands.
 * Tracks test assignments and results.
 */
public class ABTestManager {
    private final Map<String, ABTest> activeTests;
    private final Logger logger;

    public ABTestManager() {
        this.activeTests = new ConcurrentHashMap<>();
        this.logger = Logger.getLogger("AuroraCommands");
    }

    /**
     * Register an A/B test.
     *
     * @param test The test to register
     */
    public void registerTest(ABTest test) {
        activeTests.put(test.getTestName().toLowerCase(), test);
        logger.info("Registered A/B test: " + test.getTestName() +
                   " with " + test.getVariants().size() + " variants");
    }

    /**
     * Get an A/B test by name.
     *
     * @param testName Test name
     * @return The test, or null if not found
     */
    public ABTest getTest(String testName) {
        return activeTests.get(testName.toLowerCase());
    }

    /**
     * Check if a test exists.
     *
     * @param testName Test name
     * @return True if test exists
     */
    public boolean hasTest(String testName) {
        return activeTests.containsKey(testName.toLowerCase());
    }

    /**
     * Remove a test.
     *
     * @param testName Test name
     * @return True if test was removed
     */
    public boolean removeTest(String testName) {
        ABTest removed = activeTests.remove(testName.toLowerCase());
        if (removed != null) {
            logger.info("Removed A/B test: " + testName);
            return true;
        }
        return false;
    }

    /**
     * Get all active test names.
     *
     * @return Set of test names
     */
    public Set<String> getActiveTestNames() {
        return new HashSet<>(activeTests.keySet());
    }

    /**
     * Get all active tests.
     *
     * @return Map of test names to tests
     */
    public Map<String, ABTest> getAllTests() {
        return new HashMap<>(activeTests);
    }

    /**
     * Clear all tests.
     */
    public void clearAll() {
        activeTests.clear();
        logger.info("Cleared all A/B tests");
    }

    /**
     * Get a summary of all active tests.
     *
     * @return Formatted summary string
     */
    public String getAllTestsSummary() {
        if (activeTests.isEmpty()) {
            return "§eNo active A/B tests";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("§6§l=== Active A/B Tests ===\n\n");

        for (ABTest test : activeTests.values()) {
            sb.append(TestResults.generateSummary(test)).append("\n");
        }

        return sb.toString();
    }

    /**
     * Get the number of active tests.
     *
     * @return Test count
     */
    public int getTestCount() {
        return activeTests.size();
    }
}
