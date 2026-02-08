package dev.aurora.completion;

import dev.aurora.Manager.CommandManager;
import dev.aurora.struct.ArgumentType;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Enhanced tab completion with fuzzy matching, async support, and caching.
 */
public class SmartTabCompleter {
    private final CommandManager manager;
    private final CompletionCache cache;
    private static final double FUZZY_THRESHOLD = 0.6; // 60% similarity minimum

    public SmartTabCompleter(CommandManager manager) {
        this.manager = manager;
        this.cache = new CompletionCache();
    }

    /**
     * Apply fuzzy matching to filter completions.
     * Returns completions that either start with the input or are similar to it.
     *
     * @param input The user's input
     * @param completions The list of possible completions
     * @param threshold Similarity threshold (0.0 to 1.0)
     * @return Filtered and sorted completions
     */
    public List<String> fuzzyMatch(String input, List<String> completions, double threshold) {
        if (input == null || input.isEmpty()) {
            return completions;
        }

        if (completions == null || completions.isEmpty()) {
            return new ArrayList<>();
        }

        String lowerInput = input.toLowerCase();

        // Separate exact prefix matches from fuzzy matches
        List<String> prefixMatches = new ArrayList<>();
        List<String> fuzzyMatches = new ArrayList<>();

        for (String completion : completions) {
            String lowerCompletion = completion.toLowerCase();

            // Exact prefix match gets highest priority
            if (lowerCompletion.startsWith(lowerInput)) {
                prefixMatches.add(completion);
            } else {
                // Check fuzzy similarity
                double similarity = LevenshteinDistance.similarity(input, completion);
                if (similarity >= threshold) {
                    fuzzyMatches.add(completion);
                }
            }
        }

        // Sort fuzzy matches by similarity
        fuzzyMatches = fuzzyMatches.stream()
            .sorted((a, b) -> {
                double simA = LevenshteinDistance.similarity(input, a);
                double simB = LevenshteinDistance.similarity(input, b);
                return Double.compare(simB, simA);
            })
            .collect(Collectors.toList());

        // Combine: prefix matches first, then fuzzy matches
        List<String> result = new ArrayList<>(prefixMatches);
        result.addAll(fuzzyMatches);

        return result;
    }

    /**
     * Get completions with default fuzzy threshold.
     *
     * @param input The user's input
     * @param completions The list of possible completions
     * @return Filtered and sorted completions
     */
    public List<String> fuzzyMatch(String input, List<String> completions) {
        return fuzzyMatch(input, completions, FUZZY_THRESHOLD);
    }

    /**
     * Get async completions from an ArgumentType.
     * If the type supports async completions, use those. Otherwise fall back to sync.
     *
     * @param type The argument type
     * @param sender The command sender
     * @param callback Callback with the completions
     */
    public void getCompletionsAsync(ArgumentType<?> type, CommandSender sender,
                                     java.util.function.Consumer<List<String>> callback) {
        // Check if type has async method (will be added to ArgumentType interface)
        try {
            // Try to call getCompletionsAsync if it exists
            CompletableFuture<List<String>> future = type.getCompletionsAsync(sender);
            future.thenAccept(callback);
        } catch (Exception e) {
            // Fallback to synchronous completions
            callback.accept(type.getCompletions(sender));
        }
    }

    /**
     * Get cached completions for an argument type.
     *
     * @param cacheKey The cache key
     * @param type The argument type
     * @param sender The command sender
     * @return Cached or freshly computed completions
     */
    public List<String> getCachedCompletions(String cacheKey, ArgumentType<?> type,
                                            CommandSender sender) {
        return cache.get(cacheKey, () -> type.getCompletions(sender));
    }

    /**
     * Get cached completions with custom TTL.
     *
     * @param cacheKey The cache key
     * @param type The argument type
     * @param sender The command sender
     * @param ttlMillis Cache TTL in milliseconds
     * @return Cached or freshly computed completions
     */
    public List<String> getCachedCompletions(String cacheKey, ArgumentType<?> type,
                                            CommandSender sender, long ttlMillis) {
        return cache.get(cacheKey, () -> type.getCompletions(sender), ttlMillis);
    }

    /**
     * Filter completions based on sender permissions.
     *
     * @param completions The completions to filter
     * @param sender The command sender
     * @param permissionPrefix The permission prefix (e.g., "command.")
     * @return Filtered completions
     */
    public List<String> filterByPermission(List<String> completions, CommandSender sender,
                                          String permissionPrefix) {
        return completions.stream()
            .filter(completion -> sender.hasPermission(permissionPrefix + completion))
            .collect(Collectors.toList());
    }

    /**
     * Sort player suggestions by distance from the sender.
     * Closer players appear first in the suggestions.
     *
     * @param completions List of player names
     * @param sender The command sender
     * @return Sorted completions
     */
    public List<String> sortByDistance(List<String> completions, CommandSender sender) {
        // Only applicable if sender is a player
        if (!(sender instanceof org.bukkit.entity.Player)) {
            return completions;
        }

        org.bukkit.entity.Player senderPlayer = (org.bukkit.entity.Player) sender;
        org.bukkit.Location senderLoc = senderPlayer.getLocation();

        return completions.stream()
            .sorted((a, b) -> {
                org.bukkit.entity.Player playerA = org.bukkit.Bukkit.getPlayer(a);
                org.bukkit.entity.Player playerB = org.bukkit.Bukkit.getPlayer(b);

                if (playerA == null && playerB == null) return 0;
                if (playerA == null) return 1;
                if (playerB == null) return -1;

                double distA = senderLoc.distance(playerA.getLocation());
                double distB = senderLoc.distance(playerB.getLocation());

                return Double.compare(distA, distB);
            })
            .collect(Collectors.toList());
    }

    /**
     * Clear the completion cache.
     */
    public void clearCache() {
        cache.clear();
    }

    /**
     * Get the completion cache instance.
     *
     * @return The cache
     */
    public CompletionCache getCache() {
        return cache;
    }
}
