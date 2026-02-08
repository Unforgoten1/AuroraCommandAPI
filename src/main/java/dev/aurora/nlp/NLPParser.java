package dev.aurora.nlp;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Natural Language Processing parser for commands.
 * Converts natural language input into executable commands.
 *
 * Example: "give me 64 diamonds" -> "/give Player123 DIAMOND 64"
 */
public class NLPParser {
    private final List<NLPPattern> patterns;
    private final SynonymDictionary synonyms;
    private final EntityExtractor extractor;
    private final Logger logger;
    private final String triggerPrefix;

    /**
     * Create an NLP parser.
     *
     * @param triggerPrefix The prefix that activates NLP (e.g., "aurora" or "!")
     */
    public NLPParser(String triggerPrefix) {
        this.patterns = new ArrayList<>();
        this.synonyms = new SynonymDictionary();
        this.extractor = new EntityExtractor(synonyms);
        this.logger = Logger.getLogger("AuroraCommands");
        this.triggerPrefix = triggerPrefix.toLowerCase();

        registerDefaultPatterns();
    }

    /**
     * Register default command patterns.
     */
    private void registerDefaultPatterns() {
        // Give command patterns
        addPattern("give {player} {amount} {item}", "give {player} {item} {amount}");
        addPattern("give me {amount} {item}", "give {sender} {item} {amount}");
        addPattern("give {amount} {item} to {player}", "give {player} {item} {amount}");

        // Teleport patterns
        addPattern("teleport {player} to {location}", "tp {player} {location}");
        addPattern("teleport me to {player}", "tp {sender} {player}");
        addPattern("tp to {player}", "tp {sender} {player}");

        // Heal patterns
        addPattern("heal {player}", "heal {player}");
        addPattern("heal me", "heal {sender}");

        // Time patterns
        addPattern("set time to {time}", "time set {time}");
        addPattern("make it {time}", "time set {time}");

        // Weather patterns
        addPattern("make it rain", "weather rain");
        addPattern("make it sunny", "weather clear");
        addPattern("clear weather", "weather clear");

        // Gamemode patterns
        addPattern("set gamemode {mode} for {player}", "gamemode {mode} {player}");
        addPattern("gamemode {mode}", "gamemode {mode} {sender}");

        logger.info("Registered " + patterns.size() + " default NLP patterns");
    }

    /**
     * Add an NLP pattern.
     *
     * @param pattern The natural language pattern
     * @param commandTemplate The command template to generate
     */
    public void addPattern(String pattern, String commandTemplate) {
        patterns.add(new NLPPattern(pattern, commandTemplate));
        logger.info("Added NLP pattern: " + pattern + " -> " + commandTemplate);
    }

    /**
     * Check if a message is an NLP command.
     *
     * @param message The chat message
     * @return True if message starts with trigger prefix
     */
    public boolean isNLPCommand(String message) {
        return message != null && message.toLowerCase().trim().startsWith(triggerPrefix);
    }

    /**
     * Parse a natural language command.
     *
     * @param sender The command sender
     * @param message The natural language message (e.g., "aurora give me 64 diamonds")
     * @return The parsed command string, or null if no match
     */
    public String parse(CommandSender sender, String message) {
        if (!isNLPCommand(message)) {
            return null;
        }

        // Remove trigger prefix
        String input = message.trim();
        if (input.toLowerCase().startsWith(triggerPrefix)) {
            input = input.substring(triggerPrefix.length()).trim();
        }

        logger.info("Parsing NLP input: " + input);

        // Try to match against patterns
        for (NLPPattern pattern : patterns) {
            Map<String, String> rawEntities = pattern.match(input);

            if (rawEntities != null) {
                logger.info("Matched pattern: " + pattern.getPattern());
                logger.info("Raw entities: " + rawEntities);

                // Extract and normalize entities
                Map<String, String> entities = extractor.extractEntities(rawEntities);
                logger.info("Normalized entities: " + entities);

                // Replace {sender} with actual sender name
                if (sender instanceof Player) {
                    for (Map.Entry<String, String> entry : entities.entrySet()) {
                        if (entry.getValue().equals("{sender}")) {
                            entities.put(entry.getKey(), sender.getName());
                        }
                    }
                }

                // Generate command
                String command = pattern.generateCommand(entities);
                logger.info("Generated command: /" + command);

                return command;
            }
        }

        logger.info("No pattern matched for: " + input);
        return null;
    }

    /**
     * Get all registered patterns.
     *
     * @return List of patterns
     */
    public List<NLPPattern> getPatterns() {
        return new ArrayList<>(patterns);
    }

    /**
     * Get the synonym dictionary.
     *
     * @return The synonym dictionary
     */
    public SynonymDictionary getSynonyms() {
        return synonyms;
    }

    /**
     * Get the trigger prefix.
     *
     * @return The trigger prefix
     */
    public String getTriggerPrefix() {
        return triggerPrefix;
    }

    /**
     * Clear all patterns.
     */
    public void clearPatterns() {
        patterns.clear();
        logger.info("Cleared all NLP patterns");
    }

    /**
     * Get pattern count.
     *
     * @return Number of registered patterns
     */
    public int getPatternCount() {
        return patterns.size();
    }
}
