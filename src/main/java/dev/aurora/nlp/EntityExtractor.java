package dev.aurora.nlp;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Extracts typed entities from raw text.
 * Converts strings like "diamonds" to Material.DIAMOND, "64" to integer 64, etc.
 */
public class EntityExtractor {
    private final SynonymDictionary synonyms;
    private final Logger logger;

    public EntityExtractor(SynonymDictionary synonyms) {
        this.synonyms = synonyms;
        this.logger = Logger.getLogger("AuroraCommands");
    }

    /**
     * Extract and normalize entities from raw matches.
     *
     * @param rawEntities The raw entity strings from pattern matching
     * @return Normalized entity map
     */
    public Map<String, String> extractEntities(Map<String, String> rawEntities) {
        Map<String, String> normalized = new HashMap<>();

        for (Map.Entry<String, String> entry : rawEntities.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            // Normalize based on placeholder name
            String normalizedValue = normalizeEntity(key, value);
            normalized.put(key, normalizedValue);
        }

        return normalized;
    }

    /**
     * Normalize a single entity based on its type.
     *
     * @param type The entity type (placeholder name)
     * @param rawValue The raw value
     * @return Normalized value
     */
    private String normalizeEntity(String type, String rawValue) {
        switch (type.toLowerCase()) {
            case "player":
                return normalizePlayer(rawValue);

            case "item":
            case "material":
                return normalizeMaterial(rawValue);

            case "amount":
            case "number":
            case "count":
                return normalizeNumber(rawValue);

            case "world":
                return normalizeWorld(rawValue);

            default:
                // No normalization - return as-is
                return rawValue;
        }
    }

    /**
     * Normalize a player name.
     * Handles "me", "myself", or tries to find online player by partial name.
     */
    private String normalizePlayer(String rawValue) {
        // Check synonyms
        String synonym = synonyms.getSynonym("player", rawValue);
        if (synonym != null) {
            return synonym; // Returns "{sender}" for "me"
        }

        // Try to find player by name
        Player player = Bukkit.getPlayer(rawValue);
        if (player != null) {
            return player.getName();
        }

        // Try partial match
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getName().toLowerCase().startsWith(rawValue.toLowerCase())) {
                logger.info("Matched partial player name: " + rawValue + " -> " + p.getName());
                return p.getName();
            }
        }

        // Return as-is (might be offline player)
        return rawValue;
    }

    /**
     * Normalize a material/item name.
     * Handles synonyms like "diamonds" -> "DIAMOND".
     */
    private String normalizeMaterial(String rawValue) {
        // Check synonyms
        String synonym = synonyms.getSynonym("item", rawValue);
        if (synonym != null) {
            return synonym;
        }

        // Try exact material match
        try {
            Material material = Material.valueOf(rawValue.toUpperCase());
            return material.name();
        } catch (IllegalArgumentException ignored) {
        }

        // Try fuzzy matching
        String normalized = rawValue.toUpperCase()
                .replace("DIAMONDS", "DIAMOND")
                .replace("EMERALDS", "EMERALD")
                .replace("GOLDS", "GOLD_INGOT")
                .replace("IRONS", "IRON_INGOT")
                .replace("STONES", "STONE")
                .replace("WOODS", "OAK_LOG")
                .replace("S", ""); // Remove trailing 's'

        // Check if normalized matches
        try {
            Material material = Material.valueOf(normalized);
            logger.info("Fuzzy matched material: " + rawValue + " -> " + material.name());
            return material.name();
        } catch (IllegalArgumentException ignored) {
        }

        // Return as-is
        return rawValue;
    }

    /**
     * Normalize a number.
     * Handles words like "one", "two", "ten", "hundred".
     */
    private String normalizeNumber(String rawValue) {
        // Check synonyms
        String synonym = synonyms.getSynonym("number", rawValue);
        if (synonym != null) {
            return synonym;
        }

        // Try to parse as integer
        try {
            int number = Integer.parseInt(rawValue);
            return String.valueOf(number);
        } catch (NumberFormatException ignored) {
        }

        // Check for word numbers
        Map<String, Integer> wordNumbers = new HashMap<>();
        wordNumbers.put("one", 1);
        wordNumbers.put("two", 2);
        wordNumbers.put("three", 3);
        wordNumbers.put("four", 4);
        wordNumbers.put("five", 5);
        wordNumbers.put("six", 6);
        wordNumbers.put("seven", 7);
        wordNumbers.put("eight", 8);
        wordNumbers.put("nine", 9);
        wordNumbers.put("ten", 10);
        wordNumbers.put("twenty", 20);
        wordNumbers.put("thirty", 30);
        wordNumbers.put("forty", 40);
        wordNumbers.put("fifty", 50);
        wordNumbers.put("sixty", 60);
        wordNumbers.put("hundred", 100);

        Integer wordNumber = wordNumbers.get(rawValue.toLowerCase());
        if (wordNumber != null) {
            return String.valueOf(wordNumber);
        }

        // Return as-is
        return rawValue;
    }

    /**
     * Normalize a world name.
     */
    private String normalizeWorld(String rawValue) {
        // Try exact match
        if (Bukkit.getWorld(rawValue) != null) {
            return rawValue;
        }

        // Try partial match
        for (org.bukkit.World world : Bukkit.getWorlds()) {
            if (world.getName().toLowerCase().startsWith(rawValue.toLowerCase())) {
                logger.info("Matched partial world name: " + rawValue + " -> " + world.getName());
                return world.getName();
            }
        }

        // Return as-is
        return rawValue;
    }
}
