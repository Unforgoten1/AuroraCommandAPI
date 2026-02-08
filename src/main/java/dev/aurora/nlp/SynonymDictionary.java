package dev.aurora.nlp;

import java.util.HashMap;
import java.util.Map;

/**
 * Dictionary of synonyms for natural language understanding.
 * Maps informal terms to formal command arguments.
 *
 * Example: "me" -> "{sender}", "diamonds" -> "DIAMOND"
 */
public class SynonymDictionary {
    private final Map<String, Map<String, String>> synonyms;

    public SynonymDictionary() {
        this.synonyms = new HashMap<>();
        registerDefaultSynonyms();
    }

    /**
     * Register default synonyms.
     */
    private void registerDefaultSynonyms() {
        // Player synonyms
        addSynonym("player", "me", "{sender}");
        addSynonym("player", "myself", "{sender}");
        addSynonym("player", "self", "{sender}");

        // Item synonyms (common plural forms)
        addSynonym("item", "diamonds", "DIAMOND");
        addSynonym("item", "emeralds", "EMERALD");
        addSynonym("item", "golds", "GOLD_INGOT");
        addSynonym("item", "gold", "GOLD_INGOT");
        addSynonym("item", "irons", "IRON_INGOT");
        addSynonym("item", "iron", "IRON_INGOT");
        addSynonym("item", "stones", "STONE");
        addSynonym("item", "stone", "STONE");
        addSynonym("item", "woods", "OAK_LOG");
        addSynonym("item", "wood", "OAK_LOG");
        addSynonym("item", "dirt", "DIRT");
        addSynonym("item", "grass", "GRASS_BLOCK");
        addSynonym("item", "sand", "SAND");
        addSynonym("item", "gravel", "GRAVEL");
        addSynonym("item", "cobble", "COBBLESTONE");
        addSynonym("item", "cobblestone", "COBBLESTONE");
        addSynonym("item", "apple", "APPLE");
        addSynonym("item", "apples", "APPLE");
        addSynonym("item", "bread", "BREAD");
        addSynonym("item", "steak", "COOKED_BEEF");
        addSynonym("item", "beef", "COOKED_BEEF");
        addSynonym("item", "pork", "COOKED_PORKCHOP");
        addSynonym("item", "chicken", "COOKED_CHICKEN");
        addSynonym("item", "fish", "COOKED_COD");

        // Tool synonyms
        addSynonym("item", "pickaxe", "DIAMOND_PICKAXE");
        addSynonym("item", "pick", "DIAMOND_PICKAXE");
        addSynonym("item", "axe", "DIAMOND_AXE");
        addSynonym("item", "shovel", "DIAMOND_SHOVEL");
        addSynonym("item", "sword", "DIAMOND_SWORD");
        addSynonym("item", "hoe", "DIAMOND_HOE");

        // Number synonyms
        addSynonym("number", "a", "1");
        addSynonym("number", "an", "1");
        addSynonym("number", "one", "1");
        addSynonym("number", "two", "2");
        addSynonym("number", "three", "3");
        addSynonym("number", "four", "4");
        addSynonym("number", "five", "5");
        addSynonym("number", "six", "6");
        addSynonym("number", "seven", "7");
        addSynonym("number", "eight", "8");
        addSynonym("number", "nine", "9");
        addSynonym("number", "ten", "10");
        addSynonym("number", "stack", "64");
        addSynonym("number", "dozen", "12");
        addSynonym("number", "hundred", "100");

        // World synonyms
        addSynonym("world", "overworld", "world");
        addSynonym("world", "nether", "world_nether");
        addSynonym("world", "end", "world_the_end");
    }

    /**
     * Add a synonym.
     *
     * @param category The category (e.g., "player", "item", "number")
     * @param synonym The informal term
     * @param replacement The formal replacement
     */
    public void addSynonym(String category, String synonym, String replacement) {
        synonyms.computeIfAbsent(category.toLowerCase(), k -> new HashMap<>())
                .put(synonym.toLowerCase(), replacement);
    }

    /**
     * Get a synonym replacement.
     *
     * @param category The category
     * @param term The term to look up
     * @return The replacement, or null if not found
     */
    public String getSynonym(String category, String term) {
        Map<String, String> categoryMap = synonyms.get(category.toLowerCase());
        if (categoryMap == null) {
            return null;
        }
        return categoryMap.get(term.toLowerCase());
    }

    /**
     * Check if a synonym exists.
     *
     * @param category The category
     * @param term The term
     * @return True if synonym exists
     */
    public boolean hasSynonym(String category, String term) {
        return getSynonym(category, term) != null;
    }

    /**
     * Get all synonyms for a category.
     *
     * @param category The category
     * @return Map of synonyms, or empty map if category not found
     */
    public Map<String, String> getCategorySynonyms(String category) {
        Map<String, String> categoryMap = synonyms.get(category.toLowerCase());
        return categoryMap != null ? new HashMap<>(categoryMap) : new HashMap<>();
    }

    /**
     * Clear all synonyms.
     */
    public void clear() {
        synonyms.clear();
    }

    /**
     * Get the number of categories.
     *
     * @return Category count
     */
    public int getCategoryCount() {
        return synonyms.size();
    }

    /**
     * Get total number of synonyms across all categories.
     *
     * @return Total synonym count
     */
    public int getTotalSynonymCount() {
        return synonyms.values().stream()
                .mapToInt(Map::size)
                .sum();
    }
}
