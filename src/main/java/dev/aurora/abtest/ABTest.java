package dev.aurora.abtest;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents an A/B test with multiple variants.
 * Assigns players to variants and tracks results.
 */
public class ABTest {
    private final String testName;
    private final List<Variant> variants;
    private final Map<UUID, String> playerAssignments;
    private final Random random;
    private final long createdAt;

    /**
     * Create a new A/B test.
     *
     * @param testName Test name
     * @param variants List of variants to test
     */
    public ABTest(String testName, List<Variant> variants) {
        this.testName = testName;
        this.variants = new ArrayList<>(variants);
        this.playerAssignments = new ConcurrentHashMap<>();
        this.random = new Random();
        this.createdAt = System.currentTimeMillis();
    }

    /**
     * Create a simple A/B test with two variants.
     *
     * @param testName Test name
     * @param variantA First variant
     * @param variantB Second variant
     * @return New A/B test
     */
    public static ABTest create(String testName, Variant variantA, Variant variantB) {
        return new ABTest(testName, Arrays.asList(variantA, variantB));
    }

    /**
     * Get the variant assigned to a sender.
     * For players, assignment is persistent. For console, returns first variant.
     *
     * @param sender The command sender
     * @return Assigned variant
     */
    public Variant getVariant(CommandSender sender) {
        if (!(sender instanceof Player)) {
            // Console always gets first variant
            return variants.get(0);
        }

        Player player = (Player) sender;
        UUID playerId = player.getUniqueId();

        // Check if player already has an assignment
        String assignedVariantName = playerAssignments.get(playerId);
        if (assignedVariantName != null) {
            return getVariantByName(assignedVariantName);
        }

        // Assign player to a variant based on weights
        Variant assigned = selectVariantByWeight();
        playerAssignments.put(playerId, assigned.getName());

        return assigned;
    }

    /**
     * Select a variant based on weights.
     *
     * @return Selected variant
     */
    private Variant selectVariantByWeight() {
        int totalWeight = variants.stream().mapToInt(Variant::getWeight).sum();
        int randomValue = random.nextInt(totalWeight);

        int currentWeight = 0;
        for (Variant variant : variants) {
            currentWeight += variant.getWeight();
            if (randomValue < currentWeight) {
                return variant;
            }
        }

        return variants.get(0); // Fallback
    }

    /**
     * Get a variant by name.
     *
     * @param name Variant name
     * @return Variant, or first variant if not found
     */
    private Variant getVariantByName(String name) {
        return variants.stream()
                .filter(v -> v.getName().equals(name))
                .findFirst()
                .orElse(variants.get(0));
    }

    /**
     * Get all variants in this test.
     *
     * @return List of variants
     */
    public List<Variant> getVariants() {
        return new ArrayList<>(variants);
    }

    /**
     * Get the test name.
     *
     * @return Test name
     */
    public String getTestName() {
        return testName;
    }

    /**
     * Get the number of players assigned to each variant.
     *
     * @return Map of variant names to player counts
     */
    public Map<String, Integer> getDistribution() {
        Map<String, Integer> distribution = new HashMap<>();
        for (Variant variant : variants) {
            distribution.put(variant.getName(), 0);
        }

        for (String variantName : playerAssignments.values()) {
            distribution.put(variantName, distribution.getOrDefault(variantName, 0) + 1);
        }

        return distribution;
    }

    /**
     * Get when this test was created.
     *
     * @return Creation timestamp
     */
    public long getCreatedAt() {
        return createdAt;
    }

    /**
     * Clear all player assignments.
     */
    public void reset() {
        playerAssignments.clear();
    }
}
